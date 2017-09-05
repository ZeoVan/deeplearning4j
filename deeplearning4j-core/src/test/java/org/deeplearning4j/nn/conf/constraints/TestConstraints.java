package org.deeplearning4j.nn.conf.constraints;

import org.deeplearning4j.nn.api.layers.LayerConstraint;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.constraint.MaxNormConstraint;
import org.deeplearning4j.nn.conf.constraint.MinMaxNormConstraint;
import org.deeplearning4j.nn.conf.constraint.NonNegativeConstraint;
import org.deeplearning4j.nn.conf.constraint.UnitNormConstraint;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.junit.Test;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestConstraints {

    @Test
    public void testLayerCustomConstraints() throws Exception {

        LayerConstraint[] constraints = new LayerConstraint[]{
                new MaxNormConstraint(0.5, 1),
                new MinMaxNormConstraint(0.3, 0.4, 1.0, 1),
                new NonNegativeConstraint(),
                new UnitNormConstraint(1)
        };

        Set<String> customConstraint = new HashSet<>();
        customConstraint.add("RW");

        for (LayerConstraint lc : constraints) {

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .learningRate(0.0)
                    .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 5))
                    .list()
                    .layer(new LSTM.Builder().nIn(12).nOut(10)
                            .constraints(lc)
                            .build())
                    .layer(new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE).nIn(10).nOut(8).build())
                    .build();

            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();

            LayerConstraint exp = lc.clone();
            assertEquals(exp.toString(), net.getLayer(0).conf().getLayer().getConstraints().get(0).toString());

            INDArray input = Nd4j.rand(3, 12);
            INDArray labels = Nd4j.rand(3, 8);

            net.fit(input, labels);

            INDArray RW0 = net.getParam("0_RW");


            if (lc instanceof MaxNormConstraint) {
                assertTrue(RW0.norm2(1).maxNumber().doubleValue() <= 0.5);

            } else if (lc instanceof MinMaxNormConstraint) {
                assertTrue(RW0.norm2(1).minNumber().doubleValue() >= 0.3);
                assertTrue(RW0.norm2(1).maxNumber().doubleValue() <= 0.4);
            } else if (lc instanceof NonNegativeConstraint) {
                assertTrue(RW0.minNumber().doubleValue() >= 0.0);
            } else if (lc instanceof UnitNormConstraint) {
                assertEquals(RW0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(RW0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ModelSerializer.writeModel(net, baos, true);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(bais, true);

            assertEquals(net.getLayerWiseConfigurations(), restored.getLayerWiseConfigurations());
            assertEquals(net.params(), restored.params());
        }
    }

    @Test
    public void testLayerRecurrentConstraints() throws Exception {

        LayerConstraint[] constraints = new LayerConstraint[]{
                new MaxNormConstraint(0.5, 1),
                new MinMaxNormConstraint(0.3, 0.4, 1.0, 1),
                new NonNegativeConstraint(),
                new UnitNormConstraint(1)
        };

        for (LayerConstraint lc : constraints) {

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .learningRate(0.0)
                    .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 5))
                    .list()
                    .layer(new LSTM.Builder().nIn(12).nOut(10)
                            .recurrentConstraints(lc).build())
                    .layer(new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE).nIn(10).nOut(8).build())
                    .build();

            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();

            LayerConstraint exp = lc.clone();
            assertEquals(exp.toString(), net.getLayer(0).conf().getLayer().getConstraints().get(0).toString());

            INDArray input = Nd4j.rand(3, 12);
            INDArray labels = Nd4j.rand(3, 8);

            net.fit(input, labels);

            INDArray RW0 = net.getParam("0_RW");


            if (lc instanceof MaxNormConstraint) {
                assertTrue(RW0.norm2(1).maxNumber().doubleValue() <= 0.5);

            } else if (lc instanceof MinMaxNormConstraint) {
                assertTrue(RW0.norm2(1).minNumber().doubleValue() >= 0.3);
                assertTrue(RW0.norm2(1).maxNumber().doubleValue() <= 0.4);
            } else if (lc instanceof NonNegativeConstraint) {
                assertTrue(RW0.minNumber().doubleValue() >= 0.0);
            } else if (lc instanceof UnitNormConstraint) {
                assertEquals(RW0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(RW0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ModelSerializer.writeModel(net, baos, true);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(bais, true);

            assertEquals(net.getLayerWiseConfigurations(), restored.getLayerWiseConfigurations());
            assertEquals(net.params(), restored.params());
        }
    }

    @Test
    public void testLayerBiasConstraints() throws Exception {

        LayerConstraint[] constraints = new LayerConstraint[]{
                new MaxNormConstraint(0.5, 1),
                new MinMaxNormConstraint(0.3, 0.4, 1.0, 1),
                new NonNegativeConstraint(),
                new UnitNormConstraint(1)
        };

        for (LayerConstraint lc : constraints) {

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .learningRate(0.0)
                    .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 5))
                    .biasInit(10.0)
                    .list()
                    .layer(new DenseLayer.Builder().nIn(12).nOut(10)
                            .biasConstraints(lc).build())
                    .layer(new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE).nIn(10).nOut(8).build())
                    .build();

            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();

            LayerConstraint exp = lc.clone();
            assertEquals(exp.toString(), net.getLayer(0).conf().getLayer().getConstraints().get(0).toString());

            INDArray input = Nd4j.rand(3, 12);
            INDArray labels = Nd4j.rand(3, 8);

            net.fit(input, labels);

            INDArray b0 = net.getParam("0_b");


            if (lc instanceof MaxNormConstraint) {
                assertTrue(b0.norm2(1).maxNumber().doubleValue() <= 0.5);

            } else if (lc instanceof MinMaxNormConstraint) {
                assertTrue(b0.norm2(1).minNumber().doubleValue() >= 0.3);
                assertTrue(b0.norm2(1).maxNumber().doubleValue() <= 0.4);
            } else if (lc instanceof NonNegativeConstraint) {
                assertTrue(b0.minNumber().doubleValue() >= 0.0);
            } else if (lc instanceof UnitNormConstraint) {
                assertEquals(b0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(b0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ModelSerializer.writeModel(net, baos, true);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(bais, true);

            assertEquals(net.getLayerWiseConfigurations(), restored.getLayerWiseConfigurations());
            assertEquals(net.params(), restored.params());
        }
    }

    @Test
    public void testLayerWeightsConstraints() throws Exception {

        LayerConstraint[] constraints = new LayerConstraint[]{
                new MaxNormConstraint(0.5, 1),
                new MinMaxNormConstraint(0.3, 0.4, 1.0, 1),
                new NonNegativeConstraint(),
                new UnitNormConstraint(1)
        };

        for (LayerConstraint lc : constraints) {

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .learningRate(0.0)
                    .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 5))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(12).nOut(10)
                            .weightConstraints(lc).build())
                    .layer(new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE).nIn(10).nOut(8).build())
                    .build();

            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();

            LayerConstraint exp = lc.clone();
            assertEquals(exp.toString(), net.getLayer(0).conf().getLayer().getConstraints().get(0).toString());

            INDArray input = Nd4j.rand(3, 12);
            INDArray labels = Nd4j.rand(3, 8);

            net.fit(input, labels);

            INDArray w0 = net.getParam("0_W");


            if (lc instanceof MaxNormConstraint) {
                assertTrue(w0.norm2(1).maxNumber().doubleValue() <= 0.5);

            } else if (lc instanceof MinMaxNormConstraint) {
                assertTrue(w0.norm2(1).minNumber().doubleValue() >= 0.3);
                assertTrue(w0.norm2(1).maxNumber().doubleValue() <= 0.4);
            } else if (lc instanceof NonNegativeConstraint) {
                assertTrue(w0.minNumber().doubleValue() >= 0.0);
            } else if (lc instanceof UnitNormConstraint) {
                assertEquals(w0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(w0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ModelSerializer.writeModel(net, baos, true);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(bais, true);

            assertEquals(net.getLayerWiseConfigurations(), restored.getLayerWiseConfigurations());
            assertEquals(net.params(), restored.params());
        }
    }

    @Test
    public void testLayerWeightsAndBiasConstraints() throws Exception {

        LayerConstraint[] constraints = new LayerConstraint[]{
                new MaxNormConstraint(0.5, 1),
                new MinMaxNormConstraint(0.3, 0.4, 1.0, 1),
                new NonNegativeConstraint(),
                new UnitNormConstraint(1)
        };

        for (LayerConstraint lc : constraints) {

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .learningRate(0.0)
                    .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 5))
                    .biasInit(0.2)
                    .list()
                    .layer(new DenseLayer.Builder().nIn(12).nOut(10)
                            .constraints(lc).build())
                    .layer(new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE).nIn(10).nOut(8).build())
                    .build();

            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();

            LayerConstraint exp = lc.clone();
            assertEquals(exp.toString(), net.getLayer(0).conf().getLayer().getConstraints().get(0).toString());

            INDArray input = Nd4j.rand(3, 12);
            INDArray labels = Nd4j.rand(3, 8);

            net.fit(input, labels);

            INDArray w0 = net.getParam("0_W");
            INDArray b0 = net.getParam("0_b");


            if (lc instanceof MaxNormConstraint) {
                assertTrue(w0.norm2(1).maxNumber().doubleValue() <= 0.5);
                assertTrue(b0.norm2(1).maxNumber().doubleValue() <= 0.5);

            } else if (lc instanceof MinMaxNormConstraint) {
                assertTrue(w0.norm2(1).minNumber().doubleValue() >= 0.3);
                assertTrue(w0.norm2(1).maxNumber().doubleValue() <= 0.4);
                assertTrue(b0.norm2(1).minNumber().doubleValue() >= 0.3);
                assertTrue(b0.norm2(1).maxNumber().doubleValue() <= 0.4);
            } else if (lc instanceof NonNegativeConstraint) {
                assertTrue(w0.minNumber().doubleValue() >= 0.0);
                assertTrue(b0.minNumber().doubleValue() >= 0.0);
            } else if (lc instanceof UnitNormConstraint) {
                assertEquals(w0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(w0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(b0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(b0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ModelSerializer.writeModel(net, baos, true);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(bais, true);

            assertEquals(net.getLayerWiseConfigurations(), restored.getLayerWiseConfigurations());
            assertEquals(net.params(), restored.params());
        }
    }


    @Test
    public void testLayerWeightsAndBiasSeparateConstraints() throws Exception {

        LayerConstraint[] constraints = new LayerConstraint[]{
                new MaxNormConstraint(0.5, 1),
                new MinMaxNormConstraint(0.3, 0.4, 1.0, 1),
                new NonNegativeConstraint(),
                new UnitNormConstraint(1)
        };

        for (LayerConstraint lc : constraints) {

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .learningRate(0.0)
                    .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0, 5))
                    .biasInit(0.2)
                    .list()
                    .layer(new DenseLayer.Builder().nIn(12).nOut(10)
                            .weightConstraints(lc).biasConstraints(lc).build())
                    .layer(new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE).nIn(10).nOut(8).build())
                    .build();

            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();

            LayerConstraint exp = lc.clone();
            assertEquals(exp.toString(), net.getLayer(0).conf().getLayer().getConstraints().get(0).toString());

            INDArray input = Nd4j.rand(3, 12);
            INDArray labels = Nd4j.rand(3, 8);

            net.fit(input, labels);

            INDArray w0 = net.getParam("0_W");
            INDArray b0 = net.getParam("0_b");


            if (lc instanceof MaxNormConstraint) {
                assertTrue(w0.norm2(1).maxNumber().doubleValue() <= 0.5);
                assertTrue(b0.norm2(1).maxNumber().doubleValue() <= 0.5);

            } else if (lc instanceof MinMaxNormConstraint) {
                assertTrue(w0.norm2(1).minNumber().doubleValue() >= 0.3);
                assertTrue(w0.norm2(1).maxNumber().doubleValue() <= 0.4);
                assertTrue(b0.norm2(1).minNumber().doubleValue() >= 0.3);
                assertTrue(b0.norm2(1).maxNumber().doubleValue() <= 0.4);
            } else if (lc instanceof NonNegativeConstraint) {
                assertTrue(w0.minNumber().doubleValue() >= 0.0);
                assertTrue(b0.minNumber().doubleValue() >= 0.0);
            } else if (lc instanceof UnitNormConstraint) {
                assertEquals(w0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(w0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(b0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6);
                assertEquals(b0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ModelSerializer.writeModel(net, baos, true);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(bais, true);

            assertEquals(net.getLayerWiseConfigurations(), restored.getLayerWiseConfigurations());
            assertEquals(net.params(), restored.params());
        }
    }

        @Test
    public void testModelConstraints() throws Exception {

        LayerConstraint[] constraints = new LayerConstraint[]{
                new MaxNormConstraint(0.5, 1),
                new MinMaxNormConstraint(0.3, 0.4, 1.0, 1),
                new NonNegativeConstraint(),
                new UnitNormConstraint(1)
        };

        for(LayerConstraint lc : constraints){

            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .constraints(lc)
                    .learningRate(0.0)
                    .weightInit(WeightInit.DISTRIBUTION).dist(new NormalDistribution(0,5))
                    .biasInit(1)
                    .list()
                    .layer(new DenseLayer.Builder().nIn(12).nOut(10).build())
                    .layer(new OutputLayer.Builder().lossFunction(LossFunctions.LossFunction.MSE).nIn(10).nOut(8).build())
                    .build();

            MultiLayerNetwork net = new MultiLayerNetwork(conf);
            net.init();

            LayerConstraint exp = lc.clone();
            assertEquals(exp.toString(), net.getLayer(0).conf().getLayer().getConstraints().get(0).toString());
            assertEquals(exp.toString(), net.getLayer(1).conf().getLayer().getConstraints().get(0).toString());

            INDArray input = Nd4j.rand(3, 12);
            INDArray labels = Nd4j.rand(3, 8);

            net.fit(input, labels);

            INDArray w0 = net.getParam("0_W");
            INDArray w1 = net.getParam("1_W");

            if(lc instanceof MaxNormConstraint){
                assertTrue(w0.norm2(1).maxNumber().doubleValue() <= 0.5 );
                assertTrue(w1.norm2(1).maxNumber().doubleValue() <= 0.5 );
            } else if(lc instanceof MinMaxNormConstraint){
                assertTrue(w0.norm2(1).minNumber().doubleValue() >= 0.3 );
                assertTrue(w0.norm2(1).maxNumber().doubleValue() <= 0.4 );
                assertTrue(w1.norm2(1).minNumber().doubleValue() >= 0.3 );
                assertTrue(w1.norm2(1).maxNumber().doubleValue() <= 0.4 );
            } else if(lc instanceof NonNegativeConstraint ){
                assertTrue(w0.minNumber().doubleValue() >= 0.0 );
            } else if(lc instanceof UnitNormConstraint ){
                assertEquals(w0.norm2(1).minNumber().doubleValue(), 1.0, 1e-6 );
                assertEquals(w0.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6 );
                assertEquals(w1.norm2(1).minNumber().doubleValue(), 1.0, 1e-6 );
                assertEquals(w1.norm2(1).maxNumber().doubleValue(), 1.0, 1e-6 );
            }


            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ModelSerializer.writeModel(net, baos, true);
            byte[] bytes = baos.toByteArray();

            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
            MultiLayerNetwork restored = ModelSerializer.restoreMultiLayerNetwork(bais, true);

            assertEquals(net.getLayerWiseConfigurations(), restored.getLayerWiseConfigurations());
            assertEquals(net.params(), restored.params());
        }

    }

}
