import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

class Driver {
	
	// components
	
	static JFrame frame;
	
	static JPanel topPanel;
	static JButton playButton;
	static JCheckBox stopsCheckBox;
	static JComboBox<Integer> timePicker;
	
	static JMapViewer map;
	
	static BufferedImage img;
	static IconMarker arrow;
	
	static Timer timer;
	
	static Color lineColor = Color.RED;
	
	// variables
	
	static int currTime = 0;
	static int animationSpeed = 15;
	static Integer[] animationTimes = {15, 30, 45, 60};
	
	// trip
	
	static ArrayList<TripPoint> trip;
	
	 public static void main(String[] args) throws FileNotFoundException, IOException {
		
		try {
			TripPoint.readFile("triplog.csv");
			
			TripPoint.h1StopDetection();
		} catch (IOException e) {
			
		}
		
		trip = TripPoint.getTrip();
		
		// set up frame
		frame = new JFrame("Project 5 - Kevin Nguyen");
		
		frame.setPreferredSize(new Dimension(1200, 800));
		
		// play button
		playButton = new JButton("Play");
		
		// checkbox to enable / disable stops
		stopsCheckBox = new JCheckBox("Include Stops");
		
		stopsCheckBox.setSelected(true);
		
		// dropbox to select animation time
		JTextField timePickerText = new JTextField("Animation Speed:");
		timePickerText.setEditable(false);
		timePickerText.setBorder(null);
		
		timePicker = new JComboBox<Integer>(animationTimes);
		
		// add to panel
		topPanel = new JPanel();
		
		topPanel.add(playButton);
		topPanel.add(stopsCheckBox);
		topPanel.add(timePickerText);
		topPanel.add(timePicker);
		
		// set up map viewer
		map = new JMapViewer();
		
		map.setTileSource(new OsmTileSource.TransportMap());
		
		// set up map icon
		img = null;
		try {
			
		    img = ImageIO.read(new File("arrow.png"));
		    
		} catch (IOException e) {
			
		}
		
    	arrow = new IconMarker(new Coordinate(trip.get(0).getLat(), trip.get(0).getLon()), img);
    	
    	map.addMapMarker(arrow);
    	
    	// add listeners
    	
    	playButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				play();
				
			}
    		
    	});
    	
    	stopsCheckBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				trip = stopsCheckBox.isSelected() ? TripPoint.getTrip() : TripPoint.getMovingTrip();
				
			}
    		
    	});
    	
    	timePicker.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				animationSpeed = (int) timePicker.getSelectedItem();
				timer.setDelay(animationSpeed*1000 / trip.size());
				
			}
        	
        });
    	
    	// set map center and zoom level
    	
    	double minLat = TripPoint.getTrip().get(0).getLat();
    	double maxLat = TripPoint.getTrip().get(0).getLat();
    	double minLon = TripPoint.getTrip().get(0).getLon();
    	double maxLon = TripPoint.getTrip().get(0).getLon();
    	
    	for(TripPoint t : TripPoint.getTrip()) {
    		if(t.getLat() < minLat) {
    			minLat = t.getLat();
    		}else if(t.getLat() > maxLat) {
    			maxLat = t.getLat();
    		}
    		
    		if(t.getLon() < minLon) {
    			minLon = t.getLon();
    		}else if(t.getLon() > maxLon) {
    			maxLon = t.getLon();
    		}
    	}
    
    	map.setDisplayPosition(new Coordinate((minLat + maxLat)/2, (minLon + maxLon)/2), 6);
    	
    	// finish frame
    	frame.add(topPanel, BorderLayout.NORTH);
    	frame.add(map, BorderLayout.CENTER);
    	
    	frame.pack();
    	
    	frame.setLocationRelativeTo(null);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	
    	frame.setVisible(true);
    	
    	setTime(0);
		
	}
	
	public static void play() {
		
		if(timer!=null && timer.isRunning()) {
			timer.stop();
		}
		
		setTime(0);
		
		int timeDiff = animationSpeed*1000 / trip.size();
    	
    	ActionListener ticktock = new ActionListener() {
    			
    		public void actionPerformed(ActionEvent evnt) {
    					
    			if(currTime<=trip.size()*5-10) {
    				setTime(currTime + 5);
    			}else {
    				timer.stop();
    			}
    				
    		}
    	};
    	
    	timer = new Timer(timeDiff, ticktock);
    	
    	timer.start();
		
	}
	// This method sets the current time in the trip and updates the map and arrow accordingly

	public static void setTime(int t) {
		
		// Set the current time to the specified value
		currTime = t;
		
		// Remove all map polygons from the map
		map.removeAllMapPolygons();
		
		// If the time is greater than zero, draw lines between the previous and current trip points
		if(t > 0) {
			
			for(int i = 1; i < t/5; i++) {
				
				TripPoint prev = trip.get(i-1);
				TripPoint curr = trip.get(i);
				
				drawLine(prev, curr);
				
			}
			
		}
		
		// Update the orientation and position of the arrow object
		updateArrow();
		
	}
	
	// This method draws a line between two trip points on the map

	private static void drawLine(TripPoint t1, TripPoint t2) {
		
		// Create two coordinates representing the trip points
		Coordinate a = new Coordinate(t1.getLat(), t1.getLon());
		Coordinate b = new Coordinate(t2.getLat(), t2.getLon());
		
		// Define a list of coordinates representing the line to be drawn
		List<Coordinate> route = new ArrayList<Coordinate>(Arrays.asList(a, b, a));
		
		// Create a map polygon representing the line
		MapPolygonImpl mp = new MapPolygonImpl(route);
		mp.setColor(lineColor);
		
		// Add the map polygon to the map and update the display
		map.addMapPolygon(mp);
		map.setMapPolygonsVisible(true);
		map.repaint();
		
	}
	
	// This method updates the position and orientation of an arrow object based on the current time in a trip

	public static void updateArrow() {
			
		// Declare variables for two trip points
		TripPoint t1;
		TripPoint t2;
		
		// Get the current trip point based on the current time
		TripPoint curr = trip.get(currTime/5);
		
		// Determine the previous and current trip points
		if(currTime/5==0) {
			t1 = trip.get(0);
			t2 = trip.get(1);
		}else {
			t1 = trip.get(currTime/5-1);
			t2 = trip.get(currTime/5);
		}
		
		// Calculate the angle between the two trip points
		double angle = Math.toDegrees(Math.atan((t2.getLat()-t1.getLat())/(t2.getLon()-t1.getLon())));
		
		// If the current trip point is to the right of the previous trip point, add 180 degrees to the angle
		if(t2.getLon() > t1.getLon()) {
			angle += 180;
		}
		
		// Rotate the arrow object to face the direction of the current trip point
		rotateArrow(270 - angle);
		
		// Update the latitude and longitude of the arrow object to match the current trip point
		arrow.setLat(curr.getLat());
		arrow.setLon(curr.getLon());
			
	}


	public static void rotateArrow(double angle) {
		
		arrow.setImage(rotateImage(img, angle));
		
	}
	
	// This method takes a BufferedImage object and a rotation angle as input parameters
	private static BufferedImage rotateImage(BufferedImage sourceImage, double angle) {
	    // Get the width and height of the input image
	    int width = sourceImage.getWidth();
	    int height = sourceImage.getHeight();
	    
	    // Create a new BufferedImage object with the same width, height, and type as the input image
	    BufferedImage destImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	    
	    // Create a Graphics2D object from the new BufferedImage
	    Graphics2D g2d = destImage.createGraphics();

	    // Create a new AffineTransform object to represent the rotation
	    AffineTransform transform = new AffineTransform();
	    
	    // Rotate the AffineTransform object by the specified angle around the center of the image
	    transform.rotate(angle / 180 * Math.PI, width / 2 , height / 2);
	    
	    // Use the Graphics2D object to draw the rotated image onto the new BufferedImage
	    g2d.drawRenderedImage(sourceImage, transform);

	    // Release resources associated with the Graphics2D object
	    g2d.dispose();
	    
	    // Return the rotated BufferedImage object
	    return destImage;
	}

	
}