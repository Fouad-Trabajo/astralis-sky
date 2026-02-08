package jparsec.math;

import jparsec.ephem.Functions;
import jparsec.graph.DataSet;
import jparsec.util.JPARSECException;

/**
 * Fourier analysis and FFT (Fast Fourier Transform) utilities.
 * @author T. Alonso Albi - OAN (Spain)
 * @version 1.0
 */
public class Fourier {

	/** Number of input points in the data. */
	public int n;
	/** Mean value of the input data. */
	public double mean;
	/** Main terms of the analysis. */
	public Complex[] c;
	/** Frequencies of the main terms. */
	public int f[];
	
	/**
	 * Reproduces the original data with the 
	 * terms in this instance.
	 * @return Approximate original data.
	 */
	public double[] reproduceData() {
		double newData[] = new double[n];
    	double sf = n / 2.0;
    	for (int index=0;index<n;index++) {
	    	double v = 0;
	    	double cte = -Constant.TWO_PI * index / n;
	    	for (int i=0; i<c.length; i++) {
		    	double kth = cte * f[i];
		    	v += Math.cos(kth) * c[i].real;
		    	v += Math.sin(kth) * c[i].imaginary;
	    	}
	    	newData[index] = mean + v / sf;
    	}
    	return newData;
	}
	
	
	/* STATIC UTILITIES */
	
    /**
     * Compute the FFT of x.
     * @param x Set of complex.
     * @return The FFT.
     * @throws JPARSECException If the length of x is not a power of 2.
     */
    public static Complex[] fft(Complex[] x) throws JPARSECException {
        int N = x.length;

        // base case
        if (N == 1) return new Complex[] { x[0] };

        // radix 2 Cooley-Tukey FFT
        if (!isPowerOf2(N)) throw new JPARSECException("N is not a power of 2");

        // fft of even terms
        Complex[] even = new Complex[N/2];
        for (int k = 0; k < N/2; k++) {
            even[k] = x[2*k];
        }
        Complex[] q = fft(even);

        // fft of odd terms
        Complex[] odd  = even;  // reuse the array
        for (int k = 0; k < N/2; k++) {
            odd[k] = x[2*k + 1];
        }
        Complex[] r = fft(odd);

        // combine
        Complex[] y = new Complex[N];
        double c = Constant.TWO_PI / N;
        for (int k = 0; k < N/2; k++) {
            double kth = -c * k;
            Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
            y[k]       = q[k].add(wk.multiply(r[k]));
            y[k + N/2] = q[k].substract(wk.multiply(r[k]));
        }
        return y;
    }


    /**
     * Compute the inverse FFT of x.
     * @param x Set of complex.
     * @return The inverse FFT.
     * @throws JPARSECException If the length of x is not a power of 2.
     */
    public static Complex[] ifft(Complex[] x) throws JPARSECException {
        int N = x.length;

        // radix 2 Cooley-Tukey FFT
        if (!isPowerOf2(N)) throw new JPARSECException("N is not a power of 2");

        Complex[] y = new Complex[N];

        // take conjugate
        for (int i = 0; i < N; i++) {
            y[i] = x[i].conjugate();
        }

        // compute forward FFT
        y = fft(y);

        // take conjugate again
        for (int i = 0; i < N; i++) {
            y[i] = y[i].conjugate();
        }

        // divide by N
        double c = 1.0 / N;
        for (int i = 0; i < N; i++) {
            y[i] = y[i].multiply(c);
        }

        return y;

    }

    /**
     * Compute the circular convolution of x and y.
     * @param x X set of complex.
     * @param y Y set of complex.
     * @return The convolution.
     * @throws JPARSECException If the length of x is not a power of 2.
     */
    public static Complex[] circularConvolution(Complex[] x, Complex[] y) throws JPARSECException {
        if (x.length != y.length) throw new JPARSECException("Dimensions don't agree");

        int N = x.length;

        // compute FFT of each sequence
        Complex[] a = fft(x);
        Complex[] b = fft(y);

        // point-wise multiply
        Complex[] c = new Complex[N];
        for (int i = 0; i < N; i++) {
            c[i] = a[i].multiply(b[i]);
        }

        // compute inverse FFT
        return ifft(c);
    }


    /**
     * Compute the linear convolution of x and y.
     * @param x X set of complex.
     * @param y Y set of complex.
     * @return The convolution.
     * @throws JPARSECException If the length of x is not a power of 2.
     */
    public static Complex[] linearConvolution(Complex[] x, Complex[] y) throws JPARSECException {
        Complex ZERO = new Complex(0, 0);

        Complex[] a = new Complex[2*x.length];
        for (int i = 0;        i <   x.length; i++) a[i] = x[i];
        for (int i = x.length; i < 2*x.length; i++) a[i] = ZERO;

        Complex[] b = new Complex[2*y.length];
        for (int i = 0;        i <   y.length; i++) b[i] = y[i];
        for (int i = y.length; i < 2*y.length; i++) b[i] = ZERO;

        return circularConvolution(a, b);
    }

    /**
     * Returns the closest power of 2 lower or equal to the input value.
     * @param n Input value.
     * @return Closest power of 2.
     */
    public static int getClosestPowerOf2(int n) {
    	int n2 = (int) Math.pow(2.0, (int)(Math.log(n) / Math.log(2.0)));
    	if (n2 * 2 <= n) return n2 * 2;
    	return n2;
    }
    
    /**
     * Returns if the input value is a power of 2.
     * @param n Input value.
     * @return True or false.
     */
    public static boolean isPowerOf2(int n) {
    	int n2 = getClosestPowerOf2(n);
    	if (n2 != n) return false;
    	return true;
    }

    /**
     * Filters a set of data (likely a spectrum) and returns it after FFT filtering.
     * @param data The data to filter.
     * @param cutFactorRespectMax The cut factor respect the highest amplitude in the
     * FFT transform. All frequencies below an amplitude equals to max*cutFactorRespectMax,
     * likely noise, will be removed. Use 0 to cut nothing.
     * @return The filtered spectrum.
     * @throws JPARSECException If an error occurs.
     */
    public static double[] filter(double data[], double cutFactorRespectMax) throws JPARSECException {
    	int n = data.length;

    	// Apply FFT to go to complex domain
		Complex cc[] = Complex.createSetOfComplex(data);
        Complex[] ccy = Fourier.fft(cc);
		double dr[] = Complex.getSetOfReals(ccy);
		double di[] = Complex.getSetOfImaginary(ccy);

		// Compute power spectrum in physical units
    	double G[] = new double[n];
    	double sf = n / 2.0;
    	for (int i=0; i<n; i++) {
    		G[i] = Math.sqrt((di[i] * di[i] + dr[i] * dr[i])) / sf;
    	}
    	double max = DataSet.getMaximumValue(G);

		// Process the data by eliminating frequencies below the cut power
		for (int i=0; i<ccy.length; i++) {
			if (G[i] < max * cutFactorRespectMax) ccy[i] = new Complex(0, 0);
		}

		// Compute inverse FFT to recover the new data
		Complex cccy[] = ifft(ccy);
		return Complex.getSetOfReals(cccy);
    }

    /**
     * Filters a set of data (likely a spectrum) and returns it after FFT filtering.
     * @param data The data to filter.
     * @param cutFactorRespectMax The cut factor respect the highest amplitude in the
     * FFT transform. All frequencies below an amplitude equals to max*cutFactorRespectMax,
     * likely noise, will be removed. Use 0 to cut nothing.
     * @return The filtered spectrum.
     * @throws JPARSECException If an error occurs.
     */
    public static float[] filter(float data[], double cutFactorRespectMax) throws JPARSECException {
    	int n = data.length;

    	// Apply FFT to go to complex domain
		Complex cc[] = Complex.createSetOfComplex(DataSet.toDoubleArray(data));
        Complex[] ccy = Fourier.fft(cc);
		double dr[] = Complex.getSetOfReals(ccy);
		double di[] = Complex.getSetOfImaginary(ccy);

		// Compute power spectrum in physical units
    	double G[] = new double[n];
    	double sf = n / 2.0;
    	for (int i=0; i<n; i++) {
    		G[i] = Math.sqrt((di[i] * di[i] + dr[i] * dr[i])) / sf;
    	}
    	double max = DataSet.getMaximumValue(G);
    	
		// Process the data by eliminating frequencies below the cut power
		for (int i=0; i<ccy.length; i++) {
			if (G[i] < max*cutFactorRespectMax) ccy[i] = new Complex(0, 0);
		}

		// Compute inverse FFT to recover the new data
		Complex cccy[] = ifft(ccy);
		return DataSet.toFloatArray(Complex.getSetOfReals(cccy));
    }
    
    /**
     * Performs a Fourier analysis of the input data, with optional reduction to the 
     * most important terms.
     * @param originalData The input data.
     * @param cutFactor The cut factor respect the highest amplitude in the
     * FFT transform. All frequencies below an amplitude equals to max*cutFactorRespectMax,
     * likely noise, will be removed. Use 0 to cut nothing.
     * @param fixMean True to compute the mean of the input values and correct them so that 
     * mean = 0.
     * @return An object containing the number of points in the input data, the mean value 
     * of the input data, the set of Complex objects that contribute to the analysis of the 
     * input data (minus the mean value) above the cut factor, and their frequencies. Being 
     * out the output from this method an algorithm like this will reproduce the input data, 
     * except for the possible factors cut:
     * <pre>
     * double newData[] = new double[out.n];
     * double sf = n / 2.0;
     * for (int index=0;index&lt;out.n;index++) {
     *   	double v = 0;
     *    	double c = -Constant.TWO_PI * index / out.n;
     *     	for (int i=0; i&lt;out.c.length; i++) {
     *         	double kth = c * out.f[i];
     *         	v += Math.cos(kth) * out.c[i].real;
     *         	v += Math.sin(kth) * out.c[i].imaginary;
     *     	}
     *     	newData[index] = out.mean + v / sf;
     * }
     * </pre>
     * @throws JPARSECException If an error occurs.
     */
    public static Fourier fourierAnalysis(double originalData[], double cutFactor, boolean fixMean) throws JPARSECException {
    	// First step is to substract the mean value to the input data: 0-averaged array
    	// Will work with array data from now
    	int n = originalData.length;
    	double data[] = originalData.clone();
    	double mean = 0;
    	if (fixMean) {
    		mean = Functions.sumComponents(data) / n;
	    	for (int i=0;i<n;i++) {
	    		data[i] -= mean;
	    	}
    	}
    	
    	// Apply FFT to go to complex domain
		Complex cc[] = Complex.createSetOfComplex(data); // cc[i] = data[i] + i * 0
        Complex[] ccy = Fourier.fft(cc);
		double dr[] = Complex.getSetOfReals(ccy);
		double di[] = Complex.getSetOfImaginary(ccy);

		// Compute power spectrum in physical units
    	double G[] = new double[n];
    	double sf = n / 2.0;
    	for (int i=0; i<n; i++) {
    		G[i] = Math.sqrt((di[i] * di[i] + dr[i] * dr[i])) / sf;
    	}
    	double max = DataSet.getMaximumValue(G);

		// Process the data by eliminating frequencies below the cut power
    	int nt = 0;
    	for (int i=0;i<n/2;i++) {
    		if (G[i] < max * cutFactor) {
    			G[i] = 0; //dr[i] = di[i] = 0;
    			continue;
    		}
    		nt ++;
    	}
    	
    	// Create output
    	Fourier out = new Fourier();
    	out.n = n;
    	out.mean = mean;
    	out.c = new Complex[nt];
    	out.f = new int[nt];
    	int index = 0;
    	for (int i=0; i<n/2;i++) {
    		if (G[i] == 0) continue;
    		out.c[index] = ccy[i];
    		out.f[index] = i;
    		index ++;
    	}
    	
    	return out;
    }
}
