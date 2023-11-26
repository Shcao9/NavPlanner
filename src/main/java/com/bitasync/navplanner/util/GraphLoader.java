package com.bitasync.navplanner.util;

import java.io.*;
import java.util.*;

/**
 * @author Lambda
 * @date 2023/11/23
 */
public class GraphLoader
{
    public static Map<Integer, String> getInteger2location(String mapFilePath)
    {
        Map<Integer, String> integer2location = new HashMap<>();
        try(BufferedReader mapReader = new BufferedReader(new FileReader(mapFilePath));)
        {
            String line = mapReader.readLine();
            if(line.replaceAll("[^,]", "").length() != 1)
                throw new RuntimeException("文件格式错误");
            while((line = mapReader.readLine()) != null)
                integer2location.put(Integer.parseInt(line.substring(0, line.indexOf(","))), line.substring(line.indexOf(",") + 1));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return integer2location;
    }

    public static Map<String, Map<String, Double>> getGraphFromFile(String graphFilePath)
    {
        Map<String, Map<String, Double>> graph = new HashMap<>();
        try(BufferedReader graphReader = new BufferedReader(new FileReader(graphFilePath)))
        {
            String line = graphReader.readLine();
            if(line.replaceAll("[^,]", "").length() != 2)
                throw new RuntimeException("文件格式错误");
            while((line = graphReader.readLine()) != null)
            {
                String key = line.substring(0, line.indexOf(","));
                if(!graph.containsKey(key))
                    graph.put(key, new HashMap<>());
                graph.get(key).put(line.substring(line.indexOf(",") + 1, line.lastIndexOf(",")), Double.parseDouble(line.substring(line.lastIndexOf(",") + 1)));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return graph;
    }
}
