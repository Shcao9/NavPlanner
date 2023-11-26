package com.bitasync.navplanner;

import com.bitasync.navplanner.util.GeoDistanceCalculator;
import com.bitasync.navplanner.util.GraphLoader;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Lambda
 * @date 2023/11/26
 */
public class HybridPathFinder
{
    private final int ID;
    private final Set<Integer>[] INTERNAL;
    private final Map<Integer, String> INTEGER_TO_LOCATION;

    public HybridPathFinder()
    {
        this(0);
    }


    public HybridPathFinder(int id)
    {
        ID = id;
        INTERNAL = new Set[]{new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8)), new HashSet<>(Arrays.asList(9, 10, 11, 12, 13, 14)), new HashSet<>(Arrays.asList(15, 16, 17, 18, 19, 20, 21, 22, 23, 24)), new HashSet<>(Arrays.asList(25, 26, 27, 28, 29, 30)), new HashSet<>(Arrays.asList(31, 32, 33, 34, 35, 36)), new HashSet<>(Arrays.asList(37, 38, 39, 40, 41, 42)), new HashSet<>(Arrays.asList(43, 44, 45)), new HashSet<>(Arrays.asList(46, 47, 48, 49, 50, 51)), new HashSet<>(Arrays.asList(52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63))};
        INTEGER_TO_LOCATION = GraphLoader.getInteger2location("src/main/resources/csv/integer2location.csv");
    }

    public String getRoute(int source, int destination)
    {
        Stack<Integer> coarseNodes = new Dijkstra().getRoute(source, destination);
        Deque<Integer> routes = new ArrayDeque<>(coarseNodes);
        Deque<Integer> dq = new ArrayDeque<>();
        while(!routes.isEmpty())
            dq.push(routes.poll());
        ExecutorService outerExecutor = Executors.newFixedThreadPool(2);
        ReentrantLock outerLock = new ReentrantLock();
        int index = 0;
        Map<Integer, String> infos = new HashMap<>();
        while(dq.size() >= 2)
        {
            int point1 = dq.poll();
            int point2 = dq.peek();
            boolean flag = true;
            for(var set: INTERNAL)
                if(set.contains(point1) && set.contains(point2))
                {
                    flag = false;
                    final int order = index;
                    outerExecutor.execute(() ->
                        {
                            int[] idxMap = new int[set.size()];
                            int idx = 0;
                            for(var item: set)
                                idxMap[idx++] = item;
                            String mapFilePath = "src/main/resources/temp/integer2location%d#%d.csv".formatted(order, ID);
                            String pathFilePath = "src/main/resources/temp/path_data%d#%d.csv".formatted(order, ID);
                            try(BufferedWriter mapWriter = new BufferedWriter(new FileWriter(mapFilePath)))
                            {
                                mapWriter.write("code,location");
                                mapWriter.newLine();
                                for (int i = 0; i < idxMap.length; ++i)
                                {
                                    mapWriter.write(i + 1 + "," + INTEGER_TO_LOCATION.get(idxMap[i]));
                                    mapWriter.newLine();
                                }
                                if(!GeoDistanceCalculator.getDistance4Hybrid(pathFilePath, idxMap))
                                {
                                    System.out.println("path文件写入失败");
                                    System.exit(-1);
                                }
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                            int iterCount = 8;
                            ExecutorService executor = Executors.newFixedThreadPool(iterCount);
                            Map<String, Integer> info = new HashMap<>();
                            ReentrantLock lock = new ReentrantLock();
                            for(int i = 0; i < iterCount; ++i)
                            {
                                int finalI = i;
                                executor.execute(()->
                                                 {
                                                     System.out.println("起点:%s 终点:%s ### 第%d蚁群开始探索#%d".formatted(INTEGER_TO_LOCATION.get(source), INTEGER_TO_LOCATION.get(destination), finalI + 1, order));
                                                     AntColonyOptimization antColonyOptimization = new AntColonyOptimization(mapFilePath, pathFilePath);
                                                     antColonyOptimization.antColonyTSP(500);
                                                     System.out.println("起点:%s 终点:%s ### 第%d蚁群初始探索完成#%d".formatted(INTEGER_TO_LOCATION.get(source), INTEGER_TO_LOCATION.get(destination), finalI + 1, order));
                                                     int innerIterCount = 8;
                                                     Map<String, Integer> cnt = new HashMap<>();
                                                     int innerSource = -1;
                                                     int innerDestination = -1;
                                                     for(int k = 0; k < idxMap.length; ++k)
                                                     {
                                                         if(idxMap[k] == point1)
                                                             innerSource = k + 1;
                                                         if(idxMap[k] == point2)
                                                             innerDestination = k + 1;
                                                     }
                                                     for(int k = 0; k < innerIterCount; ++k)
                                                     {
                                                         String key = antColonyOptimization.getRoute(innerSource, innerDestination);
                                                         cnt.put(key, cnt.getOrDefault(key, 0) + 1);
                                                     }
                                                     String key = INTEGER_TO_LOCATION.get(point1) + "," + INTEGER_TO_LOCATION.get(point2);

                                                     String route = cnt.entrySet().stream()
                                                             .max(Map.Entry.comparingByValue())
                                                             .map(Map.Entry::getKey)
                                                             .orElse(null);
                                                     lock.lock();
                                                     info.put(route, info.getOrDefault(route, 0) + 1);
                                                     lock.unlock();
                                                 });
                            }
                            executor.shutdown();
                            try
                            {
                                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // 等待所有任务执行完毕
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                            outerLock.lock();
                            infos.put(order, info.entrySet().stream()
                                    .max(Map.Entry.comparingByValue())
                                    .map(Map.Entry::getKey)
                                    .orElse(null));
                            outerLock.unlock();
                        });
                }
            if(flag)
                infos.put(index, INTEGER_TO_LOCATION.get(point1));
            ++index;
        }
        if(!dq.isEmpty())
            infos.put(index++, INTEGER_TO_LOCATION.get(dq.poll()));
        outerExecutor.shutdown();
        try
        {
            outerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // 等待所有任务执行完毕
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < index; ++i)
        {
            String route = infos.get(i).replace("[", "").replace("]", "");
            sb.append(route).append(",");
        }
        if(!sb.isEmpty() && sb.charAt(sb.length() - 1) == ',')
            sb.replace(sb.length() - 1, sb.length(), "");
        String[] split = String.valueOf(sb).replaceAll(" ", "").split(",");
        List<String> strs = new ArrayList<>();
        Set<String> set = new HashSet<>();
        for(var str: split)
            if(!set.contains(str))
            {
                strs.add(str);
                set.add(str);
            }
        sb = new StringBuilder();
        for(int i = 0; i < strs.size(); ++i)
        {
            if(i > 0)
                sb.append("->");
            sb.append(strs.get(i));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws IOException
    {
        Map<Integer, String> integer2location = GraphLoader.getInteger2location("src/main/resources/csv/integer2location.csv");
        ExecutorService executor = Executors.newFixedThreadPool(8);
        ReentrantLock lock = new ReentrantLock();
        Map<String, String> routes = new HashMap<>();
        for(int i = 1; i <= 69; ++i)
            for(int j = 1; j <= 69; ++j)
            {
                int finalI = i;
                int finalJ = j;
                executor.execute(()->
                                 {
                                     HybridPathFinder hybridPathFinder = new HybridPathFinder(finalI * 70 + finalJ);
                                     String route = "[" + hybridPathFinder.getRoute(finalI, finalJ) + "]";
                                     lock.lock();
                                     routes.put(integer2location.get(finalI) + "," + integer2location.get(finalJ), route);
                                     lock.unlock();
                                 });
            }
        executor.shutdown();
        try
        {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS); // 等待所有任务执行完毕
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        try(BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/csv/routeByHybrid.csv")))
        {
            writer.write("source,destination,route");
            for(int i = 1; i <= 69; ++i)
                for(int j = 1; j <= 69; ++j)
                {
                    String key = integer2location.get(i) + "," + integer2location.get(j);
                    String route = routes.get(key);
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
