package com.luciddreamingapp.beta;
import org.ejml.data.DenseMatrix64F;
import org.ejml.simple.SimpleMatrix;

/**
 * A Kalman filter implemented using SimpleMatrix.  The code tends to be easier to
 * read and write, but the performance is degraded due to excessive creation/destruction of
 * memory and the use of more generic algorithms.  This also demonstrates how code can be
 * seamlessly implemented using both SimpleMatrix and DenseMatrix64F.  This allows code
 * to be quickly prototyped or to be writen either by novices or experts.
 *
 * @author Peter Abeles
 */
public class SimpleFilter{

    // kinematics description
    private SimpleMatrix F;//Transitional model, applied to the prev state
    private SimpleMatrix Q;
    private SimpleMatrix H; //Observational model maps true state space to observed state space

    // sytem state estimate
    private SimpleMatrix x;//he a posteriori state estimate at time k given observations up to and including at time k;
    private SimpleMatrix P;//a posteriori error covariance matrix (a measure of the estimated accuracy of the state estimate).


 
    public void configure(DenseMatrix64F F, DenseMatrix64F Q, DenseMatrix64F H) {
        this.F = new SimpleMatrix(F);
        this.Q = new SimpleMatrix(Q);
        this.H = new SimpleMatrix(H);
    }

 
    public void setState(DenseMatrix64F x, DenseMatrix64F P) {
        this.x = new SimpleMatrix(x);
        this.P = new SimpleMatrix(P);
    }


    public void predict() {
        // x = F x
        x = F.mult(x);

        // P = F P F' + Q
        P = F.mult(P).mult(F.transpose()).plus(Q);
    }

   
    public void update(DenseMatrix64F _z, DenseMatrix64F _R) {
        // a fast way to make the matrices usable by SimpleMatrix
        SimpleMatrix z = SimpleMatrix.wrap(_z);
        SimpleMatrix R = SimpleMatrix.wrap(_R);

        // y = z - H x
        SimpleMatrix y = z.minus(H.mult(x));

        // S = H P H' + R
        SimpleMatrix S = H.mult(P).mult(H.transpose()).plus(R);

        // K = PH'S^(-1)
        SimpleMatrix K = P.mult(H.transpose().mult(S.invert()));

        // x = x + Ky
        x = x.plus(K.mult(y));

        // P = (I-kH)P = P - KHP
        P = P.minus(K.mult(H).mult(P));
    }


    public DenseMatrix64F getState() {
        return x.getMatrix();
    }


    public DenseMatrix64F getCovariance() {
        return P.getMatrix();
    }
}