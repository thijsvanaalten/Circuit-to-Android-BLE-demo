/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package com.StretchSense.GraphingLib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.widget.ImageView;


/**
 * This class is used to create a line graph image, with an autoscale and fixed scaling features
 */
public class Multiple_Bar_Graphs {
  	//line graph variables
	final static int 	VERTICAL   = 0;
	final static int 	HORIZONTAL = 1;
	private int 		index					=	0;
	private Canvas		Frame;
	private Bitmap  	Back_Drop;
	private ImageView	graph;
	private int			Orientation = VERTICAL;
	private BarGraph [] Graphs;
	private Canvas   [] Canvas_Temp;
	private Bitmap   [] Bitmap_Temp;
	private int 		Number_of_Graphs=1;
	private boolean 	y_autoscale_min_Enabled = false;
	private boolean 	y_autoscale_max_Enabled = false;
	private int			y_max=300;
	private int 		y_min=0;
	
public Multiple_Bar_Graphs(ImageView graph,Bitmap bar_image, int Number_of_Graphs){
		
		this.graph = graph;
		this.Number_of_Graphs = Number_of_Graphs;
	//Initialise Graphs Array
		Graphs = new BarGraph[Number_of_Graphs];
		Bitmap_Temp = new Bitmap[Number_of_Graphs];
				
	//Initiate inidvidual graphs
		for (int graph_number=0;graph_number<Number_of_Graphs;graph_number++){
			Bitmap_Temp[graph_number]   = Bitmap.createBitmap((int)(.95f*graph.getWidth()/Number_of_Graphs),graph.getHeight(), Bitmap.Config.ARGB_8888);
			Graphs[graph_number]  		= new BarGraph(Bitmap_Temp[graph_number], bar_image);
			
		}
	
		
		//disable autoscaling
		enable_autoscale(false);
		//set graph scale
		set_max_min(y_min,y_max);
		
		Disconnected();
	}



	public void setOrientation(int new_Orientation){
		/**
		 * Set the orientation of the graph
		 * 
		 * Options: VERTICAL	: 0 (Default)
		 * 			HORIZONTAL  : 1 
		 * 
		 * */
		Orientation=new_Orientation;
	}

	public void Disconnected(){
		/**
		 * 
		 * When disconnected prompt the use to touch on the graph to establish the connection
		 * 
		 * **/
		Init_Backdrop(graph);
	
	}

	public void Init_Backdrop(ImageView graph){
		
		/*
		 * Init_Backdrop
		 * This function initialises the Bitmap and its canvas on which the graph is drawn to 
		 * ImageView graph:	The destination view for the graph, this provides us dimensions allowing us to scale the Image to the view size
		 */
		
		float X = graph.getWidth();
		float Y = graph.getHeight();
		float ScaleX = 500;
		float ratioXY = Y/X;
		float ScaleY = (ratioXY*ScaleX);
		
		Back_Drop		= 	Bitmap.createBitmap((int)ScaleX,(int) ScaleY, Bitmap.Config.RGB_565);
		Frame			=	new Canvas(Back_Drop);
		Frame.drawColor(Color.WHITE);
		Draw_View();
	}
	
	public void add_point(int new_point, int graph_number){
		//Add new data point to a single graph
		Graphs[graph_number].add_point(new_point,false);
		
		Draw_View();
		
	}
	
	public void add_points(float[] new_points, int number_of_graphs){
		//Add multiple new data point for all graphs
		for (int graph_number=0;graph_number<number_of_graphs;graph_number++){
		Graphs[graph_number].add_point((int)new_points[graph_number],false);
		}
		Draw_View();
		
	}
	
	
	public void enable_autoscale(boolean enable){
		for (int graph_number=0;graph_number<Number_of_Graphs;graph_number++){
			Graphs[graph_number].enable_max_autoscale(enable);
			Graphs[graph_number].enable_min_autoscale(enable);
		}
		y_autoscale_min_Enabled = enable;
		y_autoscale_max_Enabled = enable;
		
	}
	
	
	public void enable_autoscale(boolean enable_min,boolean enable_max){
		for (int graph_number=0;graph_number<Number_of_Graphs;graph_number++){
			Graphs[graph_number].enable_max_autoscale(enable_min);
			Graphs[graph_number].enable_min_autoscale(enable_max);
		}
		y_autoscale_min_Enabled = enable_min;
		y_autoscale_max_Enabled = enable_max;
	}
	
	
	public boolean is_autoscale_min_enabled(){
		return y_autoscale_min_Enabled;
	}
	
	public boolean is_autoscale_max_enabled(){
		return y_autoscale_max_Enabled;
	}
	
	public void set_max_min(int min, int max){
		for (int graph_number=0;graph_number<Number_of_Graphs;graph_number++){
			Graphs[graph_number].setMax(max/10);
			Graphs[graph_number].setMin(min/10);
		}
		y_max = max;
		y_min = min;
		
	}
	
	public int get_max(){
		return y_max;
	}

	public int get_min(){
		return y_min;
	}

	public Canvas Draw_View (){
		
		/*
		 *  Frame:			Image that lines are draw to
		 * 	points:			Circular buffer with list of recent values
		 * 	start_index: 	Current position in circular buffer write
		 * 	C:				Color of lines to be drawn (i.e. 'Color.RED')
		 *  ch:				channel selection to graph, (selected from points array)
		 */
		
			for (int graph_number=0;graph_number<Number_of_Graphs;graph_number++){
				
				
				Bitmap graphIm = Graphs[graph_number].getBitmap();
				Rect src = new Rect(0,0,graphIm.getWidth(),graphIm.getHeight());
				Rect dst = new Rect((graph_number)*Back_Drop.getWidth()/Number_of_Graphs,0,(graph_number+1)*Back_Drop.getWidth()/Number_of_Graphs,Back_Drop.getHeight());
				Frame.drawBitmap(graphIm, src, dst, null);
				//Frame.drawBitmap(, graph_number*Frame.getWidth()/Number_of_Graphs, 0, null);
			}
			Update_View();
			
			return Frame;
		
	}
	
	
	public int Get_Index(){
		/*
		 * 	Get_index()
		 *  Returns index 
		 */
		return index;
	}
	
	public int Get_Bitmap_Height(){
		return Frame.getHeight();
	}
	
	public void Update_View(){
	
		graph.setImageBitmap(Back_Drop);
		
	}

	
}

