package com.example.t1dalert;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

import java.nio.FloatBuffer;
import java.util.Collections;

final class OnnxPredictor {

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final String outputName;

    OnnxPredictor(String modelPath) {
        try {
            environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            session = environment.createSession(modelPath, options);
        } catch (Exception e) {
            throw new IllegalStateException("onnx_init_failed", e);
        }

        inputName = session.getInputNames().iterator().next();
        outputName = session.getOutputNames().iterator().next();
        validateIoShape();
    }

    float predict(float[][][] input) {
        long[] shape = new long[]{1L, input[0].length, input[0][0].length};
        float[] flat = flatten(input);
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(flat), shape);
             OrtSession.Result result = session.run(Collections.singletonMap(inputName, tensor))) {
            Object value = result.get(outputName).get().getValue();
            if (value instanceof float[][]) {
                return ((float[][]) value)[0][0];
            }
            if (value instanceof float[]) {
                return ((float[]) value)[0];
            }
            throw new IllegalStateException("unexpected_output_type");
        } catch (Exception e) {
            throw new IllegalStateException("onnx_predict_failed", e);
        }
    }

    private void validateIoShape() {
        try {
            TensorInfo inputInfo = (TensorInfo) session.getInputInfo().get(inputName).getInfo();
            long[] inputShape = inputInfo.getShape();
            if (inputShape.length != 3) {
                throw new IllegalStateException("unsupported_input_rank");
            }

            TensorInfo outputInfo = (TensorInfo) session.getOutputInfo().get(outputName).getInfo();
            long[] outputShape = outputInfo.getShape();
            if (outputShape.length < 1 || outputShape.length > 2) {
                throw new IllegalStateException("unsupported_output_rank");
            }
        } catch (OrtException e) {
            throw new IllegalStateException("onnx_io_validation_failed", e);
        }
    }

    private float[] flatten(float[][][] input) {
        int batch = input.length;
        int time = input[0].length;
        int features = input[0][0].length;
        float[] out = new float[batch * time * features];
        int idx = 0;
        for (float[][] matrix : input) {
            for (float[] row : matrix) {
                for (float v : row) {
                    out[idx++] = v;
                }
            }
        }
        return out;
    }
}
