package com.luciddreamingapp.actigraph.filter;

import org.ejml.data.DenseMatrix64F;



public class KalmanFilter {


	/**Performs kalman filtering of the signal and returns the estimated value
	 * Filter must be set up with setupFilter before this method can be called
	 * Filter predicts the value and then is updated with the new measurement
	 * @param observedVariance less variance means more confidence in the observed value
	 * @param observedValue value + noise+reading noise to be filtered
	 * @return the Kalman Filter's best guess for where the observed value really is
	 */
	public static	double KalmanFilter(SimpleFilter filter, double observedValue, double observedVariance){
			
			//Observed Readings
			DenseMatrix64F z= new DenseMatrix64F(1,1);
			z.set(0, 0, observedValue); 
			//Observed readings variance
			DenseMatrix64F R= new DenseMatrix64F(1,1);
			R.set(0, 0, observedVariance);
			
			
			filter.predict(); //calculate the filter prediction
			filter.update(z, R); //get a new state from the observed values
//System.out.println("Observed: "+observedValue+" filtered:"+filter.getState().get(0,0));
			return filter.getState().get(0,0);
		
		}
	
	public static void updateFilter(SimpleFilter filter, double observedValue, double observedVariance){
		//Observed Readings
		DenseMatrix64F z= new DenseMatrix64F(1,1);
		z.set(0, 0, observedValue); 
		//Observed readings variance
		DenseMatrix64F R= new DenseMatrix64F(1,1);
		R.set(0, 0, observedVariance);	
		
		filter.update(z, R); //get a new state from the observed values
	}
	
	
	/**Sets up the passed in filter reference with the simple 1d matrices suitable for
	 * constant evaluation
	 * 
	 * @param filter filter to be set up
	 * @param processNoiseCovarianceQ the measure of confidence you have in how much the process noise affects the reading
	 */
	public static void setupFilter(SimpleFilter filter,double processNoiseCovarianceQ){

		setupFilter(filter,processNoiseCovarianceQ,0);
		
	}
	
	/**Sets up the passed in filter reference with the simple 1d matrices suitable for
	 * constant evaluation
	 * 
	 * @param filter filter to be set up
	 * @param processNoiseCovarianceQ the measure of confidence you have in how much the process noise affects the reading
	 */
	public static void setupFilter(SimpleFilter filter,double processNoiseCovarianceQ, double prediction){

		// TODO Auto-generated method stub
		
		DenseMatrix64F F= new DenseMatrix64F(1,1);
		F.set(0, 0, 1); //1 dimensional matrix, no change is taking place
		
		//process variance
		DenseMatrix64F Q= new DenseMatrix64F(1,1);
		Q.set(0, 0, processNoiseCovarianceQ); //controls how fast the filter follows the measurement
		
		//observation model dynamics
		DenseMatrix64F H= new DenseMatrix64F(1,1);
		H.set(0, 0, 1); 
		filter.configure(F, Q, H);

		//predicted estimate
		DenseMatrix64F x= new DenseMatrix64F(1,1);
		x.set(0,0,prediction);
		//predicted estimate covariance
		DenseMatrix64F P= new DenseMatrix64F(1,1);
		P.set(0, 0, 1); 
		filter.setState(x, P);
		
	}
	
	
}
