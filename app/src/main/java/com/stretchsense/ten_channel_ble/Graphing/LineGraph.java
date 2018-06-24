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
 
package com.stretchsense.ten_channel_ble.Graphing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.widget.ImageView;

import com.stretchsense.ten_channel_ble.R;

import java.util.ArrayList;


/**
 * This class is used to create a line graph image, with an autoscale and fixed scaling features
 */
 class sensorChannel{

	int mId 			= 0;
	int mColor 			= 0;
	boolean active 		= true;
	Path mPath 			= new Path();
	int  sensorHistMax 	= 100;
	float[] sensorHist 	= new float[sensorHistMax];
	float mGraphHeight 	= 1;
	int mGraphWidth 	= 1;
	float currentPoint 	= 0;

	public sensorChannel(int id, int color, int graphWidth, int graphHeight){
		mId = id;
		mColor = color;
		mGraphWidth = graphWidth;
		mGraphHeight = graphHeight;
		for (int count=0;count<sensorHistMax;count++){
			sensorHist[count]=0;
		}
	}

	public int id(){
		return mId;
	}

	public void setmGraphWidth(int width){
		mGraphWidth = width;
	}

	public boolean isActive(){
		return active;
	}

	public boolean toggleActive(){
		active = !active;
		return active;
	}

	public void setActive(boolean isActive){
		active = isActive;
	}

	public int getColor(){

		return mColor;
	}

	public float getMin(){

		float Min =9999;

		for (int count=0;count<sensorHistMax;count++){
			if (Min>sensorHist[count]){
				Min = sensorHist[count];
			}
		}

		return Min;
	}

	public float getMax(){

		float Max =0;

		for (int count=0;count<sensorHistMax;count++){
			if (Max<sensorHist[count]){
				Max = sensorHist[count];
			}
		}

		return Max;
	}

	protected float scale_to_Graph(float points2, double min, double max)
	{
		/*
		 * scale_to_Graph
		 * This function returns a array of points that are relative to the min and max values provided
		 * If value point is between min and max it will appear on the graph
		 *
		 */

		points2 = (int) ((1-((((double) points2) - min)/(max - min)))*(double) mGraphHeight);
		return points2;
	}


	public Path drawPath(long min, long max){

		mPath = new Path();
		mPath.moveTo(0,sensorHist[0]);

		for(int count=0;count<sensorHistMax;count++){
			mPath.lineTo((mGraphWidth/sensorHistMax)*count,scale_to_Graph(sensorHist[count],min,max));
		}

		return mPath;
	}

	public float getPoint(){

		return currentPoint;
	}

	public void addPoint(float value){
		//Shuffle all data points backwards one position in array storage


			for (int count=1;count<sensorHistMax;count++){
				sensorHist[count-1] = sensorHist[count];
			}

			//Add new point to the end of the data point array
			sensorHist[sensorHistMax-1] = value;

			currentPoint = value;
	}

}

public class LineGraph extends Graph{
  	//line graph variables
	private int 		graph_width 	= 500;
	private int 		graph_height;
	public long        	global_min  	= 0;
	public long        	global_max  	= 0;
	public int[]		raw_colors 		= {R.color.graph_blue,R.color.graph_red,R.color.graph_deep_orange,R.color.graph_brown,R.color.graph_lime,R.color.graph_purple,R.color.graph_cyan,R.color.graph_amber,R.color.graph_green,R.color.graph_grey};
	private Context		context;
	public ArrayList<sensorChannel> sensor = new ArrayList<>();



	public LineGraph(ImageView graph, Context context){

		this.context 			= 	context;
		paint.setStrokeWidth(3);
		paint2.setStrokeWidth(3);
		paint2.setColor(Color.WHITE);
		this.graph = graph;
		graph_height = graph.getHeight();
		Init_Backdrop(this.graph);
	}

	public float getSensorValue(int id){


		for (sensorChannel mSensor: sensor) {
			if (mSensor.id() == id) {

				return mSensor.getPoint();
			}
		}

		return 0;
	}

	public void addSensor(int id, int color){

		sensor.add(new sensorChannel(id,raw_colors[color%raw_colors.length],graph_width,graph_height));

	}

	public void removeSensor(int id){

		for (int count=0;count<sensor.size();count++){
			if(sensor.get(count).id() == id) {
				sensor.remove(count);
			}
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

	public boolean isActive (int id){

		for (sensorChannel mSensor: sensor) {
			if (mSensor.id() == id) {

				return(mSensor.isActive());

			}
		}

	return false;

	}

	//Updated
	public void enableChannel(int id){

			for (sensorChannel mSensor: sensor) {
				if (mSensor.id() == id) {

					mSensor.setActive(true);
					break;
				}
			}

	}

	//Updated
	public void disableChannel(int id){

			for (sensorChannel mSensor: sensor) {
				if (mSensor.id() == id) {

					mSensor.setActive(false);
					break;
				}
			}

	}

	//Updated
	public boolean toggleChannel(int id){

			for (sensorChannel mSensor: sensor){
				if (mSensor.id() == id){

					return mSensor.toggleActive();
				}

		}
		return false;
	}

	//Updated
	public void addPoint(float value, int id){

		for (sensorChannel mSensor: sensor)
			if (mSensor.id() == id){
				mSensor.addPoint(value);
				break;
		}

	}


	public Canvas Draw_Graph (double min, double max, double autoscale){
		
		/*
		 *  Frame:			Image that lines are draw to
		 * 	points:			Circular buffer with list of recent values
		 * 	start_index: 	Current position in circular buffer write
		 * 	C:				Color of lines to be drawn (i.e. 'Color.RED')
		 *  ch:				channel selection to graph, (selected from points array)
		 */
			try{
				Frame.drawColor(Color.WHITE);
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

			paint.setStyle(Paint.Style.STROKE);
			paint.setStrokeWidth(2);

		//Determine global range
			if (autoscaled) {
				float Max =0;
				float Min =9999;

				for (sensorChannel mSensor: sensor) {

					if (mSensor.isActive()) {

						if (Max< mSensor.getMax()) {
							Max = mSensor.getMax()+1;
						}
						if (Min>mSensor.getMin()) {
							Min = mSensor.getMin()-2;
						}
					}
				}
				if (Min>Max){

					Min=0;
					Max=1;
				}

				//Cast to int (long)
				global_max = (long) Max;
				global_min = (long) Min;

				if (global_min<0){

					global_min=0;

				}
			}
		//Draw active channels
		for (sensorChannel mSensor: sensor) {

				if (mSensor.isActive()){

					paint.setColor(context.getResources().getColor(mSensor.getColor()));
					Frame.drawPath(mSensor.drawPath(global_min,global_max), paint);

				}

		}

			return Frame;
		
	}

	/*
	//Create path based on history index of values
	public Path  create_Path(float[] y){

		//Determine maximum and minimum scale for data set range
		float min=0, max=0;

		if (autoscale_max||autoscale_min){

			if (autoscale_min){
				min = global_min;
			}
			if (autoscale_max){
				max = global_max;
			}

		}

		//Draw path based on scaled range
		Path p = new Path();
		p.moveTo(0,y[0]);

		for(int count=0;count<datapoints;count++){
			p.lineTo((graph_width/datapoints)*count,scale_to_Graph(y[count],min,max));
		}

		return p;
	}*/

	

	public int Get_Bitmap_Height(){
		return Frame.getHeight();
	}



	public void Update_Graph(){
		Draw_Graph(minimum,maximum,1);
		graph.setImageBitmap(Back_Drop);
		
	}

}

