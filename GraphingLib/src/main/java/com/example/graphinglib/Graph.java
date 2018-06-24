package com.example.graphinglib;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.ImageView;

public class Graph {
	//shared graph variables
	final static int 		VERTICAL   = 0;
	final static int 		HORIZONTAL = 1;
	protected int 			datapoints				=   10;
	protected float 		graph_points[]			=	new float [datapoints*4];
	protected float 		raw_points[][]			=	new float [5][datapoints];
	protected int 			index					=	0;
	protected Canvas		Frame;
	protected Bitmap  		Back_Drop;
	protected ImageView		graph;
	protected Paint 		paint 					= new Paint();
	protected Paint 		paint2 					= new Paint();
	protected float[]		draw_lines;
	protected	int 		maximum=0;
	protected int			minimum=0;
	protected int			current_value = 0;
	protected boolean	   	autoscale_min = true;
	protected boolean	   	autoscale_max = true;
	protected int			channel_view  = 0;
	
	protected int			Orientation = VERTICAL;
	public SmartAutoScale SmartScale = new SmartAutoScale();
	protected boolean		No_ImageView = false;
	
	
	public void set_Visible_Channel(int channel){
		channel_view = channel; 
	}
	
	protected long[] Autoscale (float[] points2){
		
		/*
		 * Autoscale
		 * This function returns the max and min of a scale so that the range fits from 5% to 95% of the graph area
		 * returns max_min[] (max_min[0] = auto minimum, max_min[1] = auto maximum)
		 */
		
		long [] max_min = new long [2];
		
		max_min[0]=-1;
		max_min[1]=-1;
		long range =0;
		
		for (int count=0; count<datapoints; count++){
	
			if (Math.ceil(points2[count])>max_min[1]){
				max_min[1]=(long) Math.ceil(points2[count]);
			}
			if (Math.floor(points2[count])<max_min[0]){
				max_min[0]=(long) Math.floor(points2[count]);
			}
			if (max_min[0]==-1){
				max_min[0]=(long) points2[count];
			}
	
		}
		range=max_min[1]-max_min[0];
		if (max_min[0]==max_min[1]){
			range=20;
		}
		max_min[0]=(int) (max_min[0]-(range*0.05));
		if (max_min[0] < 0){
			max_min[0] = 0;
		}
		max_min[1]=(int) (max_min[1]+(range*0.05));
		
		return max_min;
	}
	
	
	public int[] max_min (){
		int [] maxmin = {maximum,minimum};
		return maxmin;
	}
	
	public void setMax(int max){
		maximum = max;
	}
	public void setMin(int min){
		minimum = min;
	}
	public void enable_max_autoscale(boolean enable){
		autoscale_max=enable;
	}
	public void enable_min_autoscale(boolean enable){
		autoscale_min=enable;
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
	
	public void Update_Graph(){
		if (!No_ImageView)
		graph.setImageBitmap(Back_Drop);
		
	}
	
	protected float scale_to_Graph(float points2, double min, double max)
	{
		/*
		 * scale_to_Graph
		 * This function returns a array of points that are relative to the min and max values provided
		 * If value point is between min and max it will appear on the graph
		 * 
		 */
		points2 = (int) ((1-((((double) points2) - min)/(max - min)))*(double) Get_Bitmap_Height());
		return points2;
	}
	
	
	
}
