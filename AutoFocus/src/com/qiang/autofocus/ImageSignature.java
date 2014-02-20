package com.qiang.autofocus;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * this class implements the image signature for computing a saliency map from a given image
 * @author qzhang53
 *
 */
public class ImageSignature
{
    // the size of frame
    private int row, col;
    // the saliency map
    private Mat sal;
    // for finding the most salient region
    private Mat score;
    // the most salient window or the focus window
    // private Rect win;
    // the spited RGB
    private List<Mat> rgb;
    // the converted opponent
    private List<Mat> opponent;
    // the Fourier coefficient
    private Mat coef;
    private List<Mat> coef_complex;
    private Mat mag;
    private Mat signature;
	
    /**
     * constructor which takes the size of image (r,c) as input
     * @param r
     * @param c
     */
	public ImageSignature(int r, int c)
	{
		row=r;
		col=c;
		sal=new Mat(row, col, CvType.CV_32F);
		score=new Mat(row, col, CvType.CV_32FC1);
		rgb=new ArrayList<Mat>(3);
		opponent=new ArrayList<Mat>(6);
		/**
		 * the opponent is a list of mat, where the first three elements 
		 * are the image in opponent color space, and the remaining three
		 * are some temporary result
		 */
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
     	opponent.add(new Mat(row, col, CvType.CV_32FC1));
		coef=new Mat(row, col, CvType.CV_32FC2);
		coef_complex=new ArrayList<Mat>(2);
		mag=new Mat(row, col,CvType.CV_32F);
		signature=new Mat(row, col,CvType.CV_32FC2);		
	}
	
	/**
	 * compute the saliency map for the given image
	 * @param img
	 */
	public void computeSaliency(Mat img)
	{
		assert(img.rows()==row && img.cols()==col);
		
		Core.split(img, rgb);
		// convert the opponent color space
		rgb2opponent(rgb.get(0),rgb.get(1),rgb.get(2), opponent);
		// apply the image signature on each channel
		imageSignature(opponent.get(0),mag,coef,coef_complex,signature);
		Core.add(sal,mag,sal);
		imageSignature(opponent.get(1),mag,coef,coef_complex,signature);
		Core.add(sal,mag,sal);
		imageSignature(opponent.get(2),mag,coef,coef_complex,signature);
		Core.add(sal,mag,sal);		
	}
	
	/**
	 * get the saliency map, we will (deep) copy the saliency mat to the provided parameter
	 * @param sal
	 */
	public void getSaliencyMap(Mat sal)
	{
		this.sal.copyTo(sal);
	}
	
	/**
	 * get the most saliency window
	 * @param img	the rgb image
	 * @param win_size	the size of window
	 * @return win	the window we found
	 */
	public void getSaliencyObject(Size win_size, Rect win)
	{
		// apply the smooth and find the salient window
		// which can be achieved with a box filter
		findSalientWindow(sal, win_size, win, score);
	}
	
	/**
	 * this function find the salient window from the image
	 * which is the window with maximal sum of salient score
	 * @param sal
	 * @return
	 */
	private void findSalientWindow(Mat sal, Size win_size, Rect win, Mat score)
	{
		Imgproc.boxFilter(sal, score, -1, win_size);
		Core.MinMaxLocResult result=Core.minMaxLoc(score);
		win.x=(int)(result.maxLoc.x-win_size.width/2);
		win.y=(int)(result.maxLoc.y-win_size.height/2);
		win.width=(int)win_size.width;
		win.height=(int)win_size.height;
	}
	
	/**
	 * this function converts rgb to opponent
	 * @param r
	 * @param g
	 * @param b
	 * @return opponent
	 */
	private void rgb2opponent(Mat r, Mat g, Mat b, List<Mat> opponent)
	{	
		// converts to float type
		r.convertTo(opponent.get(3), CvType.CV_32F);
		g.convertTo(opponent.get(4), CvType.CV_32F);
		b.convertTo(opponent.get(5), CvType.CV_32F);
		
		// convert the color space
		Core.add(opponent.get(3),opponent.get(4),opponent.get(0));
		Core.subtract(opponent.get(3), opponent.get(4), opponent.get(1));
		Core.subtract(opponent.get(5),opponent.get(0),opponent.get(2));
		Core.add(opponent.get(3),opponent.get(0),opponent.get(0));
	}
	
	/**
	 * this function compute the saliency map with image signature
	 * @param img
	 * @return
	 */
	private void imageSignature(Mat img, Mat mag, Mat coef, List<Mat> coef_complex, Mat signature)
	{
		// forward dft
		Core.dft(img,coef,Core.DFT_COMPLEX_OUTPUT,img.rows());
		// unify the magnitude
		
		Core.split(coef,coef_complex);
		
		Core.magnitude(coef_complex.get(0), coef_complex.get(1), mag);
		Core.divide(coef_complex.get(0),mag,coef_complex.get(0));
		Core.divide(coef_complex.get(1),mag,coef_complex.get(1));
		Core.merge(coef_complex, coef);
		// apply the inverse dft
		
		Core.dft(coef,signature,Core.DCT_INVERSE,img.rows());
		// take square and magnitude
		Core.split(signature, coef_complex);
		Core.magnitude(coef_complex.get(0), coef_complex.get(1), mag);
		Core.multiply(mag, mag, mag);
	}
}