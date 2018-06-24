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
public class BarGraph extends Graph{
	private Bitmap		bar_image;
	float current_value  = 0;
	
public BarGraph(ImageView graph, Bitmap icon){
		
		datapoints				=   10;
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
			raw_points[0][count]	= 0;
		}
		paint.setStrokeWidth(3);
		paint2.setStrokeWidth(3);
		paint2.setColor(Color.WHITE);
		this.graph = graph;
		bar_image = icon;
		Disconnected();
	}

public BarGraph(Bitmap backdrop, Bitmap bar){
	
	/*
	 * init_Line_Graph
	 * This function initialises the array points into the format required for the drawLines function
	 * [x0,y0,x1,y1] [x1,y1,x2,y2]
	 */
	
	No_ImageView = true;
	for (int count=0;count<datapoints;count++){
		graph_points[count*4+1]	= 0;
		graph_points[count*4+3]	= 0;
		graph_points[count*4]=count*500/datapoints;
		graph_points[count*4+2]=(count+1)*500/datapoints;
		raw_points[0][count]		= 0;
	}
	paint.setStrokeWidth(3);
	paint2.setStrokeWidth(3);
	paint2.setColor(Color.WHITE);
	Back_Drop = backdrop;
	bar_image = bar;
	Frame		= new Canvas(Back_Drop);
	Draw_Graph_Lines();
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
		float ScaleY = 1500;
		float ratioXY = X/Y;
		float ScaleX = (ratioXY*ScaleY);
	
		Back_Drop		= 	Bitmap.createBitmap((int)ScaleX,(int) ScaleY, Bitmap.Config.RGB_565);
		Frame			=	new Canvas(Back_Drop);
		Draw_Graph_Lines();
		Update_Graph();
	}
	
	private void Draw_Graph_Lines(){
		//Draw graph background/lines to create view
		Frame.drawColor(Color.WHITE);
		Paint outline = new Paint();
		outline.setStrokeWidth(4);
		outline.setColor(Color.GRAY);
		Frame.drawLine(0, 0, 0, Frame.getHeight(), outline);
		Frame.drawLine(0, 0, Frame.getWidth(), 0, outline);
		Frame.drawLine(Frame.getWidth()-1, 0, Frame.getWidth()-1, Frame.getHeight(), outline);
		Frame.drawLine(0, Frame.getHeight()-1, Frame.getWidth(), Frame.getHeight()-1, outline);
		Paint divisions = new Paint();
		divisions.setColor(Color.LTGRAY);
		divisions.setStrokeWidth(3);
		Frame.drawLine(0, Frame.getHeight()/2, Frame.getWidth(), Frame.getHeight()/2, divisions);
		Frame.drawLine(0, Frame.getHeight()/4, Frame.getWidth(), Frame.getHeight()/4, divisions);
		Frame.drawLine(0, (3*Frame.getHeight())/4, Frame.getWidth(), (3*Frame.getHeight())/4, divisions);
		
	}
	
	public void add_point(int new_point, boolean autoscale){
		
		current_value=new_point;
		raw_points[0][index]=current_value;
		Draw_Graph(Color.BLUE,minimum,maximum,autoscale);
		
	}
	
	public void add_point(float new_point, boolean autoscale){
		
		current_value=  new_point;
		raw_points[0][index]=current_value;
		Draw_Graph(Color.BLUE,minimum,maximum,autoscale);
		
	}
	
	public void resetAutoscale(){
		//Reset Autoscale Range
		SmartScale.ResetScale();
	}
	
	public Canvas Draw_Graph (int C, double min, double max, boolean autoscale){
		
		/*
		 *  Frame:			Image that lines are draw to
		 * 	points:			Circular buffer with list of recent values
		 * 	start_index: 	Current position in circular buffer write
		 * 	C:				Color of lines to be drawn (i.e. 'Color.RED')
		 *  ch:				channel selection to graph, (selected from points array)
		 */
			try{
				Draw_Graph_Lines();
			}
			catch(Exception e){}
			
			boolean autoscaled = autoscale;
			
			//draw_lines=populate_points_Array(raw_points, index, minimum, maximum, autoscale_min,autoscale_max);
			//paint.setColor(C);
			//Frame.drawLines(draw_lines, paint);
			
			
			if (autoscale_max||autoscale_min){
				long[] scale = SmartScale.AutoScale(raw_points[0]);
				
				if (autoscale_min){
				min = scale[0];
				}
				if (autoscale_max){
				max = scale[1];
				}
			
			}
			maximum=(int) max;
			minimum=(int) min;
			
			Rect src  = new Rect(0,0,bar_image.getWidth(),bar_image.getHeight());
			Rect dst  = new Rect ((int)(Frame.getWidth()*0.33),(int)(Frame.getHeight()-(Frame.getHeight()*(current_value-min)/(float)(max-min))),(int)(Frame.getWidth()*0.67),Frame.getHeight());		
			
			Frame.drawBitmap(bar_image, src, dst, null);
			
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



	public Bitmap getBitmap() {

		return Back_Drop;
	}
	
	
}

