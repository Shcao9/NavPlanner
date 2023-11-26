package com.bitasync.navplanner;

import com.bitasync.navplanner.util.GeoDistanceCalculator;
import com.bitasync.navplanner.util.GeocodingUtil;
import com.bitasync.navplanner.util.GraphLoader;
import com.bitasync.navplanner.util.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.util.*;

@SpringBootApplication
public class NavplannerApplication {

	public static void main(String[] args) {
//		SpringApplication.run(NavplannerApplication.class, args);
		System.out.println("hello maven");


//		GeocodingUtil.geocode();
//		GeoDistanceCalculator.getDistance4ACS();
		GeoDistanceCalculator.getDistance4Dijkstra();
//		AntColonyOptimization antColonyOptimization = new AntColonyOptimization();
	}
//27 119.192837,26.051444
//29 119.192721,26.051059
//31 119.192260,26.050610
}
