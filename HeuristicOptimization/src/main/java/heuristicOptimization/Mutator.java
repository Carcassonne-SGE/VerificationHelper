package heuristicOptimization;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import model.heuristic.MutateRange;

public class Mutator<T extends Record> {

    private final Class<T> typeData;

    public Mutator(Class<T> typeData) {
        this.typeData = typeData;
    }

    public List<T> randomPopulation(int count, Random rand) {

        var list = new ArrayList<T>(count);
        for (int i = 0; i < count; i++) {
            list.add(createRandom(typeData, rand));
        }
        return list;
    }

    public <R> List<R> crossOver(Class<R> recordType, List<?> pop, Random rand, float scale, float mutStep) {
        try {
            RecordComponent[] components = recordType.getRecordComponents();
            int numFields = components.length;
            int popSize = pop.size();

            Object[][] paramColumns = new Object[numFields][popSize];

            for (int j = 0; j < numFields; j++) {
                RecordComponent comp = components[j];
                Class<?> fieldType = comp.getType();

                MutateRange range = comp.getAnnotation(MutateRange.class);

                if (range != null) {
                    float[] parentValues = new float[popSize];
                    for (int i = 0; i < popSize; i++) {
                        parentValues[i] = ((Number) comp.getAccessor().invoke(pop.get(i))).floatValue();
                    }
                    paramColumns[j] = numericCrossoverAndMutate(parentValues, range,rand,scale,mutStep);

                } else {
                    List<Object> nestedPopulation = new ArrayList<>(popSize);
                    for (Object individual : pop) {
                        nestedPopulation.add(comp.getAccessor().invoke(individual));
                    }
                    List<?> crossedNestedRecords = crossOver(fieldType, nestedPopulation,rand,scale,mutStep);
                    paramColumns[j] = crossedNestedRecords.toArray();
                }
            }

            var paramTypes = java.util.Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class[]::new);
            var canonicalConstructor = recordType.getDeclaredConstructor(paramTypes);
            canonicalConstructor.setAccessible(true);

            List<R> newGeneration = new ArrayList<>(popSize);
            for (int i = 0; i < popSize; i++) {
                Object[] instanceArgs = new Object[numFields];
                for (int j = 0; j < numFields; j++) {
                    instanceArgs[j] = paramColumns[j][i];
                }
                newGeneration.add(canonicalConstructor.newInstance(instanceArgs));
            }
            return newGeneration;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Float[] numericCrossoverAndMutate(float[] parents, MutateRange range, Random rand, float scale, float mutStep) {
        int n = parents.length;
        Float[] children = new Float[n];

        float minBound = range.min();
        float maxBound = range.max();

        for (int i = 0; i < n; i++) {
            float p1 = parents[rand.nextInt(n)];
            float p2 = parents[rand.nextInt(n)];

            float u = rand.nextFloat();
            float x = scale * (u - 0.5f);
            float alpha = 1.0f / (1.0f + (float) Math.exp(-x));
            float childValue = alpha * p1 + (1.0f - alpha) * p2;

            if (rand.nextFloat() < 0.15f) {

                if (rand.nextFloat() < 0.10f) {
                    childValue = minBound + rand.nextFloat() * (maxBound - minBound);
                } else {
                    if(rand.nextFloat() < 0.50) {
                        float noise = (float) rand.nextGaussian() * mutStep;
                        childValue += noise;
                    }
                    if(rand.nextFloat() < 0.50) {
                        float noise = (float) rand.nextGaussian()* ((range.max()-range.min()));
                        childValue += noise;
                    }

                }

                // Ganz wichtig: Sicherstellen, dass wir die @MutateRange-Grenzen nicht sprengen!
                if (childValue < minBound) childValue = minBound;
                if (childValue > maxBound) childValue = maxBound;
            }

            children[i] = childValue;
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    private <R> R createRandom(Class<R> recordType, Random rand){
        try {
            var constructor = recordType.getDeclaredConstructors()[0];
            var parameters = constructor.getParameterAnnotations();
            var paraTypes =constructor.getParameterTypes();
            var objects = new Object[parameters.length];
            for (int j = 0; j < parameters.length; j++) {
                var paras = parameters[j];
                boolean foundMutRange = false;
                for(var para : paras){
                    if(para instanceof MutateRange range) {
                        float mut = rand.nextFloat() * (range.max() - range.min()) + range.min();
                        float v = mut;
                        objects[j] = v;
                        foundMutRange = true;
                    }
                }
                if(!foundMutRange) {
                    objects[j] = createRandom(paraTypes[j],rand);
                }
            }
           return (R) constructor.newInstance(objects);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
