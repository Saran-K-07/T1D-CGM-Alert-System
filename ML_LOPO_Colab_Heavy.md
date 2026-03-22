# BiTMAML LOPO-CV Heavy Pipeline (Colab)

Use this flow when you want full LOPO-CV quality and Android-ready export artifacts.

## Cell 1: Environment

```python
!pip -q install pandas numpy scikit-learn torch onnx onnxruntime onnxscript

import json
import pickle
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import mean_absolute_error, mean_squared_error

import torch
import torch.nn as nn
from torch.utils.data import DataLoader, TensorDataset

import onnx
import onnxruntime as ort
```

## Cell 2: Paths + knobs

```python
from pathlib import Path

IN_COLAB = True
try:
    from google.colab import drive
except ImportError:
    IN_COLAB = False

if IN_COLAB:
    drive.mount("/content/drive", force_remount=False)
    PROJECT_ROOT = Path("/content")
else:
    PROJECT_ROOT = Path.cwd()

DATASETS_DIR = PROJECT_ROOT / "drive/MyDrive/Dataset/n=183 OpenAPS Data Commons 2022 UNZIPPED"
PROCESSED_DIR = PROJECT_ROOT / "Processed"
RUNTIME_DIR = PROCESSED_DIR / "runtime"

PROCESSED_DIR.mkdir(parents=True, exist_ok=True)
RUNTIME_DIR.mkdir(parents=True, exist_ok=True)

WINDOW_SIZE = 36
HORIZON_60 = 12
BATCH_SIZE = 128
EPOCHS = 80
LR = 1e-3
MODEL_VERSION = "bitmaml_1h_v1"
USE_AMP = torch.cuda.is_available()

FEATURE_COLS = [
    "glucose_level", "meal", "bolus", "exercise", "hypoevent",
    "stressors", "sin_hour", "cos_hour", "isnight", "ismealtime"
]

print(DATASETS_DIR, DATASETS_DIR.exists())
```

## Cell 3: Preprocessing helpers

```python
def is_mealtime(hour: int) -> int:
    return int((7 <= hour <= 10) or (12 <= hour <= 15) or (18 <= hour <= 21))

def create_sliding_windows(df_norm, window_size=36, prediction_horizon=12):
    values = df_norm[FEATURE_COLS].values.astype(np.float32)
    glucose = df_norm["glucose_level"].values.astype(np.float32)
    X, y = [], []
    max_i = len(df_norm) - window_size - prediction_horizon + 1
    for i in range(max_i):
        X.append(values[i:i + window_size])
        y.append(glucose[i + window_size + prediction_horizon - 1])
    if not X:
        return np.empty((0, window_size, len(FEATURE_COLS)), np.float32), np.empty((0,), np.float32)
    return np.stack(X), np.array(y, np.float32)
```

## Cell 4: Model

```python
class BiTBackbone(nn.Module):
    def __init__(self, input_dim=10, lstm_hidden=64, n_heads=4, n_transformer_layers=2, dropout=0.2):
        super().__init__()
        self.bilstm = nn.LSTM(
            input_size=input_dim,
            hidden_size=lstm_hidden,
            num_layers=1,
            batch_first=True,
            bidirectional=True,
            dropout=0.0
        )
        enc = nn.TransformerEncoderLayer(
            d_model=2*lstm_hidden,
            nhead=n_heads,
            dim_feedforward=4*lstm_hidden,
            dropout=dropout,
            batch_first=True,
            activation="relu"
        )
        self.transformer = nn.TransformerEncoder(enc, num_layers=n_transformer_layers)
        self.fc_out = nn.Sequential(
            nn.Linear(2*lstm_hidden, 64), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(64, 32), nn.ReLU(), nn.Dropout(dropout),
            nn.Linear(32, 1)
        )

    def forward(self, x):
        lstm_out, _ = self.bilstm(x)
        trans_out = self.transformer(lstm_out)
        last_step = trans_out[:, -1, :]
        return self.fc_out(last_step)
```

## Cell 5: LOPO-CV train/eval

```python
def run_fold(X_train, y_train, X_val, y_val, device):
    model = BiTBackbone(input_dim=X_train.shape[2]).to(device)
    opt = torch.optim.Adam(model.parameters(), lr=LR)
    crit = nn.MSELoss()
    sch = torch.optim.lr_scheduler.ReduceLROnPlateau(opt, patience=6, factor=0.5)

    train_loader = DataLoader(TensorDataset(
        torch.tensor(X_train, dtype=torch.float32),
        torch.tensor(y_train, dtype=torch.float32).view(-1, 1)
    ), batch_size=BATCH_SIZE, shuffle=True, num_workers=2, pin_memory=torch.cuda.is_available())

    val_loader = DataLoader(TensorDataset(
        torch.tensor(X_val, dtype=torch.float32),
        torch.tensor(y_val, dtype=torch.float32).view(-1, 1)
    ), batch_size=512, shuffle=False, num_workers=2, pin_memory=torch.cuda.is_available())

    scaler = torch.cuda.amp.GradScaler(enabled=USE_AMP)
    best_val = float("inf")
    best_state = None

    for _ in range(EPOCHS):
        model.train()
        for xb, yb in train_loader:
            xb, yb = xb.to(device), yb.to(device)
            opt.zero_grad(set_to_none=True)
            with torch.cuda.amp.autocast(enabled=USE_AMP):
                pred = model(xb)
                loss = crit(pred, yb)
            scaler.scale(loss).backward()
            torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            scaler.step(opt)
            scaler.update()

        model.eval()
        val_loss = 0.0
        with torch.no_grad():
            for xb, yb in val_loader:
                xb, yb = xb.to(device), yb.to(device)
                pred = model(xb)
                val_loss += crit(pred, yb).item()
        val_loss /= max(1, len(val_loader))
        sch.step(val_loss)

        if val_loss < best_val:
            best_val = val_loss
            best_state = {k: v.detach().cpu().clone() for k, v in model.state_dict().items()}

    model.load_state_dict(best_state)
    model.eval()
    with torch.no_grad():
        pred = model(torch.tensor(X_val, dtype=torch.float32).to(device)).cpu().numpy().reshape(-1)
    return pred, best_state
```

## Cell 6: Final train + export (Android-ready)

```python
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# Load processed arrays produced by your preprocessing cells
X = np.load(PROCESSED_DIR / "final_X_60min.npy")
y = np.load(PROCESSED_DIR / "final_y_60min.npy")

model = BiTBackbone(input_dim=X.shape[2]).to(device)
opt = torch.optim.Adam(model.parameters(), lr=LR)
crit = nn.MSELoss()
loader = DataLoader(TensorDataset(
    torch.tensor(X, dtype=torch.float32),
    torch.tensor(y, dtype=torch.float32).view(-1, 1)
), batch_size=BATCH_SIZE, shuffle=True, num_workers=2, pin_memory=torch.cuda.is_available())

for _ in range(EPOCHS):
    model.train()
    for xb, yb in loader:
        xb, yb = xb.to(device), yb.to(device)
        opt.zero_grad(set_to_none=True)
        pred = model(xb)
        loss = crit(pred, yb)
        loss.backward()
        torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
        opt.step()

best_path = PROCESSED_DIR / "best_bitmaml.pth"
torch.save(model.state_dict(), best_path)

# Export ONNX
export_model = BiTBackbone(input_dim=X.shape[2])
export_model.load_state_dict(torch.load(best_path, map_location="cpu"))
export_model.eval()

onnx_path = RUNTIME_DIR / "model.onnx"
dummy = torch.randn(1, X.shape[1], X.shape[2], dtype=torch.float32)
torch.onnx.export(
    export_model,
    dummy,
    str(onnx_path),
    input_names=["input"],
    output_names=["pred"],
    dynamic_axes={"input": {0: "batch"}, "pred": {0: "batch"}},
    opset_version=18,
    do_constant_folding=True,
    dynamo=False,
)

onnx.checker.check_model(onnx.load(str(onnx_path)))

with open(PROCESSED_DIR / "final_scaler.pkl", "rb") as f:
    scaler = pickle.load(f)

metadata = {
    "model_version": MODEL_VERSION,
    "model_file": "model.onnx",
    "window_size": int(X.shape[1]),
    "horizon_steps": HORIZON_60,
    "feature_order": FEATURE_COLS,
    "output": {"name": "predicted_sgv_norm", "unit_after_denorm": "mg/dL"},
    "scaler": {
        "type": "minmax",
        "data_min": scaler.data_min_.tolist(),
        "data_max": scaler.data_max_.tolist(),
        "scale": scaler.scale_.tolist(),
        "min_offset": scaler.min_.tolist(),
    }
}

with open(RUNTIME_DIR / "metadata.json", "w") as f:
    json.dump(metadata, f, indent=2)

print("Exported:", onnx_path)
print("Exported:", RUNTIME_DIR / "metadata.json")
```

## Cell 7: Copy to Android assets

```python
# Update this path to your checked-out Android project root
ANDROID_APP_ASSETS = Path("/content/T1DAlert2/app/src/main/assets/ml")
ANDROID_APP_ASSETS.mkdir(parents=True, exist_ok=True)

import shutil
shutil.copy2(RUNTIME_DIR / "model.onnx", ANDROID_APP_ASSETS / "model.onnx")
shutil.copy2(RUNTIME_DIR / "metadata.json", ANDROID_APP_ASSETS / "metadata.json")

print("Copied runtime artifacts to:", ANDROID_APP_ASSETS)
```

## Notes

- LOPO-CV can take many hours; run on GPU runtime in Colab.
- Keep exported IO names as `input` and `pred`.
- Keep metadata `model_version` aligned with app config.
