package com.bitasync.navplanner;

import com.bitasync.navplanner.util.GeoDistanceCalculator;
import com.bitasync.navplanner.util.GraphLoader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Double.NaN;

/**
 * @author Lambda
 * @date 2023/11/24
 */
public class AntColonyOptimization
{
    private final double INITIAL_PHEROMONE = 1.0; //初始信息素
    private final double ALPHA = 1; //信息素重要度
    private final double BETA = 5; //启发因子，距离的重要程度
    private final double EVAPORATION = 0.5; //蒸发系数
    private final double Q = 500; //信息素增加强度
    private final double ANT_FACTOR = 0.8;
    private final double RANDOM_FACTOR = 0.01; //随机选择下一个城市的概率
    private final int NUMBER_OF_CITIES; //城市数量
    private final int NUMBER_OF_ANTS; //蚂蚁数量
    private final Map<Integer, String> INTEGER_TO_LOCATION;
    private final Map<String, Map<String, Double>> GRAPH;
//    private double graph[][]; //距离矩阵
    private double[][] trails; //信息素矩阵
    private Ant[] ants; //蚂蚁数组

    public AntColonyOptimization()
    {
        this("src/main/resources/csv/integer2location.csv", "src/main/resources/csv/path_data4ACS.csv");
    }

    public AntColonyOptimization(String mapFile, String pathFile)
    {
        INTEGER_TO_LOCATION = GraphLoader.getInteger2location(mapFile);
        GRAPH = GraphLoader.getGraphFromFile(pathFile);
        NUMBER_OF_CITIES = GRAPH.size();
        //蚂蚁数量系数
        NUMBER_OF_ANTS = (int)(NUMBER_OF_CITIES * ANT_FACTOR);
//        NUMBER_OF_ANTS = 1;
        ants = new Ant[NUMBER_OF_ANTS]; //蚂蚁数组
        for(int i = 0; i < NUMBER_OF_ANTS; ++i)
            ants[i] = new Ant(NUMBER_OF_CITIES + 1);
        trails = new double[NUMBER_OF_CITIES + 1][NUMBER_OF_CITIES + 1];
        for(var trail: trails)
            Arrays.fill(trail, INITIAL_PHEROMONE);
//        System.out.println("------------------------------------");
    }

    // Ant类表示一只蚂蚁
    private class Ant
    {
        private boolean[] visitedCities;
        private List<Integer> trail;//走过的路径
        private double totalDistance;

        //初始化
        public Ant(int cities)
        {
            visitedCities = new boolean[cities];
            trail = new ArrayList<>();
        }

        //清除蚂蚁的信息，为下一次迭代准备
        protected void clear()
        {
            totalDistance = -1;
            Arrays.fill(visitedCities, false);
            trail = new ArrayList<>();
        }
        //返回路径
        protected List<Integer> getTrail() { return trail; }
        //返回路径长度
        protected double getTotalDistance() {  return totalDistance; }
        //访问城市
        protected void visitCity(int city)
        {
            visitedCities[city] = true;
            trail.add(city);
        }

        //尝试访问城市
        protected boolean tryToVisit(int city)
        {
            return !visitedCities[city];
        }

        //根据距离和信息素计算选择某个城市的概率  graph[trail[trailSize - 1]][city]
        protected double getSelectionProbability(int city)
        {

            double tau = Math.pow(trails[trail.get(trail.size() - 1)][city], ALPHA);
            double distance = GRAPH.get(INTEGER_TO_LOCATION.get(trail.get(trail.size() - 1))).get(INTEGER_TO_LOCATION.get(city));
            double eta = Math.pow(1.0 / (distance < 1e-256 ? Double.MAX_VALUE: distance), BETA);
//            try
//            {
//                if(Double.isNaN(distance))
//                    throw new RuntimeException("ee");
//                if(Double.isNaN(eta))
//                    throw new RuntimeException("ee");
//                if(Double.isNaN(tau * eta))
//                    throw new RuntimeException("ee");
//            }
//            catch (Exception e)
//            {
//                System.out.println(222);
//                System.out.println(Math.pow(trails[trail.get(trail.size() - 1)][city], ALPHA));
//            }
            return tau * eta;
        }

        //访问下一个城市
        protected int selectNextCity()
        {
            if (Math.random() < RANDOM_FACTOR)
            {
                //使用纯随机选择
                int t = (int) (Math.random() * NUMBER_OF_CITIES) + 1;
                for (int i = 0; i <= NUMBER_OF_CITIES; ++i) {
                    int idx = (t + i) % (NUMBER_OF_CITIES + 1);
                    if (tryToVisit(idx))
                    {
                        if(idx == 0)
                            for(int j = 1; j <= NUMBER_OF_CITIES; ++j)
                                if(tryToVisit(j))
                                {
                                    idx = j;
                                    break;
                                }
                        return idx;
                    }
                }
            }

            //使用概率选择
            double[] probabilities = new double[NUMBER_OF_CITIES + 1];
            double prob = 0.0;
            for (int i = 1; i <= NUMBER_OF_CITIES; ++i)
            {
                if (tryToVisit(i))
                {
                    probabilities[i] = getSelectionProbability(i);
                    prob += probabilities[i];
//                    try
//                    {
//                        if(Double.isNaN(prob))
//                            throw new RuntimeException("eee");
//                    }
//                    catch (Exception e)
//                    {
//                        System.out.println(111);
//                    }
                }
            }

            //随机选择一只火蚁
            prob *= Math.random();
            int i;
            for (i = 1; i < NUMBER_OF_CITIES; ++i) {
                if (tryToVisit(i))
                {
                    prob -= probabilities[i];
                    if (prob <= 0.0)
                        break;
                }
            }

            return i;
        }
        //计算路径长度
        protected void calculateTotalDistance()
        {
            if(totalDistance == -1)
            {
                double dist = 0.0;
                for (int i = 0; i < trail.size() - 1; i++)
                    dist += GRAPH.get(INTEGER_TO_LOCATION.get(trail.get(i))).get(INTEGER_TO_LOCATION.get(trail.get(i + 1)));
                totalDistance = dist;
            }
        }
    }

    public void antColonyTSP(int iterCount)
    {
        Lock lock = new ReentrantLock();
        while(iterCount-- > 0)
        {
            ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_ANTS);
            for (int i = 0; i < NUMBER_OF_ANTS; ++i)
            {
                Ant ant = ants[i];
                executor.execute(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ant.clear();
                            ant.visitCity((int)(Math.random() * (NUMBER_OF_CITIES)) + 1);
                            while(ant.trail.size() < NUMBER_OF_CITIES)
                            {
                                int nextCity = ant.selectNextCity();
                                ant.visitCity(nextCity);
                            }
                            // 执行具体的任务逻辑
                        }
                    });
            }
            executor.shutdown();
            try
            {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // 等待所有任务执行完毕
            }
            catch (InterruptedException e)
            {
                System.out.println("error1");
            }
            for(int i = 0; i <= NUMBER_OF_CITIES; ++i)
                for(int j = 0; j <= NUMBER_OF_CITIES; ++j)
                    trails[i][j] *= (1 - EVAPORATION);
            executor = Executors.newFixedThreadPool(NUMBER_OF_ANTS);
            for (int i = 0; i < NUMBER_OF_ANTS; ++i)
            {
                Ant ant = ants[i];
                executor.execute(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            for(int i = 0; i < ant.trail.size() - 1; ++i)
                            {
                                lock.lock();
                                try
                                {
                                    double distance = GRAPH.get(INTEGER_TO_LOCATION.get(ant.trail.get(i))).get(INTEGER_TO_LOCATION.get(ant.trail.get(i + 1)));
                                    trails[ant.trail.get(i)][ant.trail.get(i + 1)] += Q / (distance == 0.0 ? 1000: distance);
                                }
                                finally
                                {
                                    lock.unlock();
                                }
                            }
                        }
                    });
            }
            executor.shutdown();
            try
            {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // 等待所有任务执行完毕
            }
            catch (InterruptedException e)
            {
                System.out.println("error2");
            }
        }

    }

    public String getRoute(int source, int destination)
    {
//        System.out.println("ok");
//        int index = 0;
        Map<String, Double> cnt = new HashMap<>();
        for(var ant: ants)
        {
//            System.out.print("ant" + index++ + ": ");
            ant.clear();
            ant.visitCity(source);
            while(ant.trail.size() < NUMBER_OF_CITIES)
            {
                int nextCity = ant.selectNextCity();
                ant.visitCity(nextCity);
                if(nextCity == destination)
                {
                    ant.calculateTotalDistance();
//                    System.out.println(ant.getTotalDistance());
                    String key = ant.trail.stream().map(INTEGER_TO_LOCATION::get).toList().toString();
                    cnt.put(key, cnt.getOrDefault(key, 0.0) + 1 / ant.getTotalDistance());
//                    System.out.println(key);
                    break;
                }
            }
        }
        return cnt.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public static void main(String[] args)
    {
        Map<Integer, String> integer2location = GraphLoader.getInteger2location("src/main/resources/csv/integer2location.csv");
        Map<String, Map<String, Integer>> routes = new HashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(integer2location.size());
        ReentrantLock lock1 = new ReentrantLock();
        for(int iter = 0; iter < 16; ++iter)
        {
            int finalIter = iter;
            executor.execute(
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        AntColonyOptimization antColonyOptimization = new AntColonyOptimization();
                        antColonyOptimization.antColonyTSP(10000);
                        ReentrantLock lock2 = new ReentrantLock();
                        System.out.println("第%d蚁群初始探索完成".formatted(finalIter + 1));
                        ExecutorService innerExecutor = Executors.newFixedThreadPool(integer2location.size());
                        for(int i = 1; i <= 69; ++i)
                            for (int j = 1; j <= 69; ++j)
                            {
                                int finalI = i;
                                int finalJ = j;
                                innerExecutor.execute(
                                    new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Map<String, Integer> cnt = new HashMap<>();
                                            for(int k = 0; k < 10; ++k)
                                            {
                                                lock2.lock();
                                                String key = antColonyOptimization.getRoute(finalI, finalJ);
                                                lock2.unlock();
                                                cnt.put(key, cnt.getOrDefault(key, 0) + 1);
                                            }
                                            String key = integer2location.get(finalI) + "," + integer2location.get(finalJ);
                                            String route = cnt.entrySet().stream()
                                                    .max(Map.Entry.comparingByValue())
                                                    .map(Map.Entry::getKey)
                                                    .orElse(null);
                                            lock1.lock();
                                            if(!routes.containsKey(key))
                                                routes.put(key, new HashMap<>());
                                            Map<String, Integer> stringIntegerMap = routes.get(key);
                                            stringIntegerMap.put(route, stringIntegerMap.getOrDefault(route, 0) + 1);
                                            lock1.unlock();
                                        }
                                    });
                            }
                        innerExecutor.shutdown();
                        try
                        {
                            innerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // 等待所有任务执行完毕
                        }
                        catch (InterruptedException e)
                        {
                            System.out.println("error4");
                        }
                        System.out.println("第%d蚁群完成".formatted(finalIter + 1));
                    }
                });
        }

        executor.shutdown();
        try
        {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // 等待所有任务执行完毕
        }
        catch (InterruptedException e)
        {
            System.out.println("error3");
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/csv/routeByACS.csv", true)))
        {
            for(int i = 1; i <= 69; ++i)
                for(int j = 1; j <= 69; ++j)
                {
                    String key = integer2location.get(i) + "," + integer2location.get(j);
                    String route = routes.get(key).entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    writer.newLine();
                    writer.write(key + "," + route);
                    writer.flush();
                }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("文件写入完成");
    }
}
