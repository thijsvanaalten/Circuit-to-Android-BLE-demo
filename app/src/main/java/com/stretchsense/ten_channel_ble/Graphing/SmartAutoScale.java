package com.stretchsense.ten_channel_ble.Graphing;

public class SmartAutoScale {
	// An autoscaling method that will retain it's maximum and minimum constraints until they are bettered (Higher/Lower)
	
	//Theory
	// Use an 100 point buffer to average the population value if this is greater/less than the max/min, this is the new autoscale range.
	// This will take 2.5s when the circuit V2 is running at a full 40 Hz or 1s with the circuit V1 running at 100Hz
	
	private float Maximum = 0;
	private float Minimum = 0;
	private int Samples = 0;
	
	
	public void ResetScale(){
		// Input:	-
		// Output:  -
		// Fuction:	Returns the Maximum and Minimum to zero so that the Maximum/Minimum can be recalculated 
		//			i.e. if the sensor is disconnected or a new sensor with different sensitivity is connected
		
		Maximum =0;
		Minimum =0;
		
	}
	
	
	long smart_min = 0;
	long smart_max = 0;
	boolean use_smart_max_min = false;
	
	public void set_smart_max_min(long min,long max){
		smart_min  = min;
		smart_max  = max;
		use_smart_max_min = true;
	}
	
	public long[] AutoScale(float[] datapoints){
	// Input:	 Datapoints buffer, contains the 100 most recent values recieved from the sensing circuit
	// Output:	 Int [2] containing the Maximum and Minimum population average of the circuit since the last reset
		
		if (Samples<datapoints.length-1){
			Samples++;
		}
		long [] max_min = new long [2];
		
		long range =0;
		float average=0;
		
		//Get population average, (Use sample count to ensure buffer is fulled/ no zeros are counted)
		for (int count=0; count<Samples; count++){
	
				average += datapoints[count]/((float)Samples);
	
		}
		// Compare to previous Maximum & Minimum popuation averages
		if (average>Maximum){
			Maximum = average;
		}
		if (average<Minimum||Minimum<=0){
			Minimum  = average;
		}
		max_min[0]=(long) Minimum;
		max_min[1]=(long) Maximum;
		
		if (use_smart_max_min){
		
			if (max_min[0]>smart_min){
				max_min[0]=smart_min;
			}
			if (max_min[1]<smart_max){
				max_min[1]=smart_max;
			}
			
		}
		range=max_min[1]-max_min[0];
		if (range<20){
			range=20;
			Maximum = Maximum+20;
			max_min[1] = (long) Maximum;
		}
		
		
		max_min[0]=(int) (max_min[0]-(range*0.05));
		if (max_min[0] < 0){
			max_min[0] = 0;
		}
		max_min[1]=(int) (max_min[1]+(range*0.05));
		
		return max_min;
	}
	
	
	
}
