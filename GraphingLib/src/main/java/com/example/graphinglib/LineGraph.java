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
 
package com.example.graphinglib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Rect;
import android.widget.ImageView;


/**
 * This class is used to create a line graph image, with an autoscale and fixed scaling features
 */
public class LineGraph extends Graph{
  	//line graph variables
	private Bitmap		Stretchsenseicon;
	
	
public LineGraph(ImageView graph, Bitmap icon){
		
			datapoints				=   100;
			graph_points			=	new float [datapoints*4];
			raw_points				=	new float [5][datapoints];
		/*
		 * init_Line_Graph
		 * This function initialises the array points into the format required for the drawLines function
		 * [x0,y0,x1,y1] [x1,y1,x2,y2]
		 */

		for (int count=0;count<datapoints;count++){
			graph_points[count*4+1]	= 0;
			graph_points[count*4+3]	= 0;
			graph_points[count*4]=count*500/datapoints;
			graph_points[count*4+2]=(count+1)*500/datapoints;
			raw_points[0][count]   = 0;
		}
		paint.setStrokeWidth(3);
		paint2.setStrokeWidth(3);
		paint2.setColor(Color.WHITE);
		this.graph = graph;
		Stretchsenseicon = icon;
		Disconnected();
	}

	public void Disconnected(){
		/**
		 * 
		 * When disconnected prompt the use to touch on the graph to establish the connection
		 * 
		 * **/
		
		Init_Backdrop(graph);
		if (Stretchsenseicon!=null){
		Paint text_paint	=	new Paint();
		text_paint.setTextAlign(Align.CENTER);
		text_paint.setColor(Color.WHITE);
		text_paint.setTextSize(40);
		Rect src = new Rect (0, 0, Stretchsenseicon.getWidth(), Stretchsenseicon.getHeight());
		Rect dst = new Rect (Frame.getWidth()/2-Frame.getHeight()/4, Frame.getHeight()/4-30, Frame.getWidth()/2+Frame.getHeight()/4, 3*Frame.getHeight()/4-30);
		Frame.drawColor(Color.argb(75,00,00,00));
		Frame.drawBitmap(Stretchsenseicon, src, dst, null);
		Frame.drawColor(Color.argb(20,00,00,00));
		Frame.drawText("Tap to Connect", Frame.getWidth()/2, 3*Frame.getHeight()/4+20, text_paint);
		}
		
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
		Paint outline = new Paint();
		outline.setColor(Color.GRAY);
		Frame.drawLine(0, 0, 0, Frame.getHeight(), outline);
		Frame.drawLine(0, 0, Frame.getWidth(), 0, outline);
		Frame.drawLine(Frame.getWidth()-1, 0, Frame.getWidth()-1, Frame.getHeight(), outline);
		Frame.drawLine(0, Frame.getHeight()-1, Frame.getWidth(), Frame.getHeight()-1, outline);
		Paint divisions = new Paint();
		divisions.setColor(Color.LTGRAY);
		Frame.drawLine(0, Frame.getHeight()/2, Frame.getWidth(), Frame.getHeight()/2, divisions);
		Frame.drawLine(0, Frame.getHeight()/4, Frame.getWidth(), Frame.getHeight()/4, divisions);
		Frame.drawLine(0, (3*Frame.getHeight())/4, Frame.getWidth(), (3*Frame.getHeight())/4, divisions);
		Update_Graph();
	}
	
	public void add_point(int new_point){
		
		current_value=new_point;
		raw_points[0][index]=current_value;
		Draw_Graph(minimum,maximum,1,channel_view);
		
	}
	
	public void add_points(int[] new_point){
		
		current_value=new_point[channel_view];
		for (int count=0;count<new_point.length;count++){
			raw_points[count][index]=new_point[count];
		}

		enable_min_autoscale(true);
		enable_max_autoscale(true);
		Draw_Graph(minimum,maximum,1,channel_view);
		
		
	}
	
	public void add_points(float[] new_point){
		
		current_value=(int) new_point[channel_view];
		for (int count=0;count<new_point.length;count++){
			raw_points[count][index] = new_point[count];
		}

		//enable_min_autoscale(true);
		//enable_max_autoscale(true);
		Draw_Graph(minimum,maximum,1,channel_view);
		
		
	}
	
	public void add_points(int[] new_point,int [] range){
		
		current_value=new_point[channel_view];
		for (int count=0;count<new_point.length;count++){
			raw_points[count][index]=new_point[count];
		}
		enable_min_autoscale(false);
		enable_max_autoscale(false);
		Draw_Graph(range[0],range[1],0,channel_view);
	}
	
	public void add_points(float[] new_point,int [] range){
		
		current_value=(int) new_point[channel_view];
		for (int count=0;count<new_point.length;count++){
			raw_points[count][index]=new_point[count];
		}
		enable_min_autoscale(false);
		enable_max_autoscale(false);
		Draw_Graph(range[0],range[1],0,channel_view);
	}

	int[] C = {Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW,Color.BLACK,Color.RED,Color.BLUE,Color.GREEN,Color.YELLOW,Color.BLACK};

	public Canvas Draw_Graph (double min, double max, double autoscale,int channel_view){
		
		/*
		 *  Frame:			Image that lines are draw to
		 * 	points:			Circular buffer with list of recent values
		 * 	start_index: 	Current position in circular buffer write
		 * 	C:				Color of lines to be drawn (i.e. 'Color.RED')
		 *  ch:				channel selection to graph, (selected from points array)
		 */
			try{
				Frame.drawColor(Color.WHITE);
				Paint outline = new Paint();
				outline.setColor(Color.GRAY);
				Frame.drawLine(0, 0, 0, Frame.getHeight(), outline);
				Frame.drawLine(0, 0, Frame.getWidth(), 0, outline);
				Frame.drawLine(Frame.getWidth()-1, 0, Frame.getWidth()-1, Frame.getHeight(), outline);
				Frame.drawLine(0, Frame.getHeight()-1, Frame.getWidth(), Frame.getHeight()-1, outline);
				Paint divisions = new Paint();
				divisions.setColor(Color.LTGRAY);
				Frame.drawLine(0, Frame.getHeight()/2, Frame.getWidth(), Frame.getHeight()/2, divisions);
				Frame.drawLine(0, Frame.getHeight()/4, Frame.getWidth(), Frame.getHeight()/4, divisions);
				Frame.drawLine(0, (3*Frame.getHeight())/4, Frame.getWidth(), (3*Frame.getHeight())/4, divisions);
			}
			catch(Exception e){}
			
			boolean autoscaled = false;
			if (autoscale==1){
				autoscaled = true;
			}
			
			
			

			for (int count=0;count<10;count++) {

				Path draw_path = new Path();
				paint.setColor(C[count]);
				Frame.drawPath(draw_path, paint);
			}
			Paint text = new Paint();
			text.setTextSize(30);
			text.setColor(Color.BLACK);
			
			Paint text_current_value = new Paint();
			text_current_value.setTextSize(30);
			text_current_value.setColor(Color.BLACK);
			text_current_value.setTextAlign(Align.RIGHT);
		
			Update_Graph();
			Increment_Index();
			
			return Frame;
		
	}
	
	public float []  populate_points_Array(float[] points2, int start_index, double min, double max, boolean autoscale_min, boolean autoscale_max){
	
		/*
		 * populate_points_Array
		 * This function populates the array points into the format required for the drawLines function
		 * [x0,y0,x1,y1] [x1,y1,x2,y2]
		 */
		
		
		if (autoscale_max||autoscale_min){
			long[] scale = Autoscale(points2);
			
			if (autoscale_min){
			min = scale[0];
			}
			if (autoscale_max){
			max = scale[1];
			}
		
		}
		maximum=(int) max;
		minimum=(int) min;
		
		float [] draw_lines = this.graph_points;
		
		for (int count=start_index;count<datapoints-1;count++){
			draw_lines[(count-start_index)*4+1]	= scale_to_Graph(points2[count],min,max);
			draw_lines[(count-start_index)*4+3]	= scale_to_Graph(points2[count+1],min,max);
		}
			draw_lines[(datapoints-1-start_index)*4+1]	= scale_to_Graph(points2[datapoints-1],min,max);
			draw_lines[(datapoints-1-start_index)*4+3]	= scale_to_Graph(points2[0],min,max);
		
		for (int count=0;count<start_index;count++){
			draw_lines[(count+datapoints-start_index)*4+1]	= scale_to_Graph(points2[count],min,max);
			draw_lines[(count+datapoints-start_index)*4+3]	= scale_to_Graph(points2[count+1],min,max);
		}
		
		
		draw_lines[1]=draw_lines[3];
		
		return draw_lines;
	}
	
	
	
	public int Get_Bitmap_Height(){
		return Frame.getHeight();
	}
	
	public void Update_Graph(){
					
		graph.setImageBitmap(Back_Drop);
		
	}

	
	
	public void Increment_Index(){
		/*	Increment_index
		 * 
		 *  increment buffer index one position
		 *  reset back to zero once it has reached the end
		 */
		
		index++;
		if (index>=datapoints){
			index=0;
		}
	}
	
	
}

