package com.bitasync.navplanner;

import com.bitasync.navplanner.util.GraphLoader;

import java.awt.*;
import java.awt.image.ImageProducer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Lambda
 * @date 2023/11/26
 */
public class Dijkstra
{
    private final Map<Integer, String> INTEGER_TO_LOCATION;
    private final Map<String, Integer> LOCATION_TO_INTEGER;
    private final Map<String, Map<String, Double>> GRAPH;
    private final int NUMBER_OF_CITIES;

    {
        INTEGER_TO_LOCATION = GraphLoader.getInteger2location("src/main/resources/csv/integer2location.csv");
        LOCATION_TO_INTEGER = new HashMap<>();
        for(var entry: INTEGER_TO_LOCATION.entrySet())
            LOCATION_TO_INTEGER.put(entry.getValue(), entry.getKey());
        GRAPH = GraphLoader.getGraphFromFile("src/main/resources/csv/path_data4Dijkstra.csv");
        NUMBER_OF_CITIES = GRAPH.size();
    }

    public String getRouteString(int source, int destination)
    {
        Stack<Integer> stk = getRoute(source, destination);
        StringBuilder route = new StringBuilder();
        while(!stk.isEmpty())
            route.append(INTEGER_TO_LOCATION.get(stk.pop())).append(",");
        return route.isEmpty() ? "" : String.valueOf(route.replace(route.length() - 1, route.length(), ""));
    }

    public Stack<Integer> getRoute(int source, int destination)
    {
        double[] costs = new double[NUMBER_OF_CITIES + 1];
        Arrays.fill(costs, Double.MAX_VALUE);
        int[] parents = new int[NUMBER_OF_CITIES + 1];
        Arrays.fill(parents, Integer.MIN_VALUE);
        boolean[] visited = new boolean[NUMBER_OF_CITIES + 1];
        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[1]));
        visited[source] = true;
        parents[source] = -1;
        costs[source] = 0;
        pq.add(new double[]{source, costs[source]});
        while(!pq.isEmpty())
        {
            var pair = pq.poll();
            int cur = (int)pair[0];
            for(var entry: GRAPH.get(INTEGER_TO_LOCATION.get(cur)).entrySet())
            {
                int next = LOCATION_TO_INTEGER.get(entry.getKey());
                if(visited[next])
                    continue;
                if(costs[next] > pair[1] + entry.getValue())
                {
                    costs[next] = pair[1] + entry.getValue();
                    parents[next] = cur;
                }
                visited[next] = true;
                pq.add(new double[]{next, costs[next]});
            }
        }
        Stack<Integer> stk = new Stack<>();
        while(destination != -1 && parents[destination] != Integer.MIN_VALUE)
        {
            stk.push(destination);
            destination = parents[destination];
        }
        return stk;
    }

    public static void main(String[] args)
    {
        Map<Integer, String> integer2location = GraphLoader.getInteger2location("src/main/resources/csv/integer2location.csv");
        Map<String, String> routes = new HashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(integer2location.size() * integer2location.size());
        ReentrantLock lock = new ReentrantLock();
        for(int i = 1; i <= 69; ++i)
            for(int j = 1; j <= 69; ++j)
            {
                int finalI = i;
                int finalJ = j;
                executor.execute(
                    new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            String key = integer2location.get(finalI) + "," + integer2location.get(finalJ);
                            String route = new Dijkstra().getRouteString(finalI, finalJ);
                            lock.lock();
                            routes.put(key, route);
                            lock.unlock();
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
            System.out.println("error");
        }
        try(BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/csv/routeByDijkstra.csv", true)))
        {
            for(int i = 1; i <= 69; ++i)
                for(int j = 1; j <= 69; ++j)
                {
                    String key = integer2location.get(i) + "," + integer2location.get(j);
                    writer.newLine();
                    writer.write(key + ",[" + routes.get(key) + "]");
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