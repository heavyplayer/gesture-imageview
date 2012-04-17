package com.polites.android;

import android.graphics.PointF;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class GestureImageViewTouchListener implements OnTouchListener {
	
	private GestureImageView image;
	
	private final PointF current = new PointF();
	private final PointF last = new PointF();
	private final PointF next = new PointF();
	private final PointF midpoint = new PointF();
	
	private final VectorF scaleVector = new VectorF();
	private final VectorF pinchVector = new VectorF();
	
	boolean touched = false;
	
	private float initialDistance;
	private float lastScale = 1.0f;
	private float currentScale = 1.0f;
	
	private float boundaryLeft = 0;
	private float boundaryTop = 0;
	private float boundaryRight = 0;
	private float boundaryBottom = 0;
	
	private float maxScale = 5.0f;
	private float minScale = 0.75f;
	private float doubleTapScale = maxScale/2.0f;
	
	private float centerX = 0;
	private float centerY = 0;
	
	private float startingScale = 0;
	
	private boolean canDragX = false;
	private boolean canDragY = false;
	
	private boolean multiTouch = false;
	
	private int displayWidth;
	private int displayHeight;
	
	private int imageWidth;
	private int imageHeight;
	
	private float velocityX;
	private float velocityY;
	
	private FlingAnimation flingAnimation;
	private GestureDetector gestureDetector;
	private GestureListener gestureListener;
	private GestureImageViewListener imageListener;

	public GestureImageViewTouchListener(GestureImageView image, int displayWidth, int displayHeight) {
		super();
		
		this.image = image;
		
		this.displayWidth = displayWidth;
		this.displayHeight = displayHeight;
		
		this.centerX = (float) displayWidth / 2.0f;
		this.centerY = (float) displayHeight / 2.0f;
		
		this.imageWidth = image.getImageWidth();
		this.imageHeight = image.getImageHeight();
		
		startingScale = image.getScale();
		
		currentScale = startingScale;
		lastScale = startingScale;
		
		// Calc boundaries
		boundaryRight = displayWidth;
		boundaryBottom = displayHeight;
		boundaryLeft = 0;
		boundaryTop = 0;
		
		next.x = image.getX();
		next.y = image.getY();
		
		flingAnimation = new FlingAnimation();
		
		flingAnimation.setListener(new FlingAnimationListener() {
			@Override
			public void onMove(float x, float y) {
				handleDrag(current.x + x, current.y + y);
			}
		});
		
		gestureListener = new GestureListener();
		gestureDetector = new GestureDetector(image.getContext(), gestureListener);
		
		imageListener = image.getGestureImageViewListener();
		
		calculateBoundaries();
	}
	
	private void startFling() {
		flingAnimation.setVelocityX(velocityX);
		flingAnimation.setVelocityY(velocityY);
		image.animationStart(flingAnimation);
	}
	
	private void stopFling() {
		image.animationStop();
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		gestureDetector.onTouchEvent(event);
		
		switch(event.getAction()) {
		
			case MotionEvent.ACTION_UP:
				multiTouch = false;
				
				initialDistance = 0;
				lastScale = currentScale;
				
				if(!canDragX) {
					next.x = centerX;
				}
				
				if(!canDragY) {
					next.y = centerY;
				}
				
				boundCoordinates();
				
				image.setScale(currentScale);
				image.setPosition(next.x, next.y);
				
				if(imageListener != null) {
					imageListener.onScale(currentScale);
					imageListener.onPosition(next.x, next.y);
				}
				
				image.redraw();
				
				break;
				
			case MotionEvent.ACTION_DOWN:
				stopFling();
				
				last.x = event.getX();
				last.y = event.getY();
				
				if(imageListener != null) {
					imageListener.onTouch(last.x, last.y);
				}
				
				touched = true;
				
				break;
				
			case MotionEvent.ACTION_MOVE:
				if(event.getPointerCount() > 1) {
					multiTouch = true;
					
					if(initialDistance > 0) {
						pinchVector.set(event);
						pinchVector.calculateLength();
						
						float distance = pinchVector.length;
						
						// We have moved (scaled)
						if(initialDistance != distance) {
							currentScale = (distance / initialDistance) * lastScale;
							
							if(currentScale > maxScale) {
								currentScale = maxScale;	
							}
							else if (currentScale < minScale) {
								currentScale = minScale;
							}
							
							calculateBoundaries();
							
							scaleVector.length *= currentScale;
							
							scaleVector.calculateEndPoint();
							
							scaleVector.length /= currentScale;
							
							next.x = scaleVector.end.x;
							next.y = scaleVector.end.y;
							
							image.setScale(currentScale);
							image.setPosition(next.x, next.y);
							
							if(imageListener != null) {
								imageListener.onScale(currentScale);
								imageListener.onPosition(next.x, next.y);
							}
							
							image.redraw();
						}
					}
					else {
						initialDistance = MathUtils.distance(event);
						
						MathUtils.midpoint(event, midpoint);
						
						scaleVector.setStart(midpoint);
						scaleVector.setEnd(next);
						
						scaleVector.calculateLength();
						scaleVector.calculateAngle();
						
						scaleVector.length /= lastScale;
					}
				}
				else {
					if(!touched) {
						touched = true;
						last.x = event.getX();
						last.y = event.getY();
						next.x = image.getX();
						next.y = image.getY();
					}
					else if(!multiTouch) {
						if(handleDrag(event.getX(), event.getY())) {
							image.redraw();
						}
					}
				}
				
				break;
				
			default:
				break;
		}
		
		return true;
	}
	
	class GestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			if(coordinatesOutsideDrawable(e.getX(), e.getY())) {
				imageListener.onTouchOutsideDrawable(e.getX(), e.getY());
			}
			else {
				imageListener.onTouchDrawable(e.getX(), e.getY());
			}
			
			return true;
		}
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if(image.getScale() == getStartingScale())
				image.setScale(getDoubleTapScale());
			else
				image.reset();
			
			initialDistance = 0;
			lastScale = image.getScale();
			currentScale = image.getScale();
			next.x = image.getX();
			next.y = image.getY();
			calculateBoundaries();
			
			return true;
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
			if(e2.getPointerCount() == 1) {
				velocityX = vX;
				velocityY = vY;
				
				startFling();
				
				return true;
			}
			
			return false;
		}
	}
	
	protected boolean handleDrag(float x, float y) {
		current.x = x;
		current.y = y;
		
		float diffX = (current.x - last.x);
		float diffY = (current.y - last.y);
		
		if(diffX != 0 || diffY != 0) {
			
			if(canDragX) next.x += diffX;
			if(canDragY) next.y += diffY;
			
			boundCoordinates();
			
			last.x = current.x;
			last.y = current.y;
			
			if(canDragX || canDragY) {
				image.setPosition(next.x, next.y);
				
				if(imageListener != null) {
					imageListener.onPosition(next.x, next.y);
				}					
				
				return true;
			}
		}
		
		return false;
	}
	
	public void reset() {
		currentScale = startingScale;
		next.x = centerX;
		next.y = centerY;
		calculateBoundaries();
		image.setScale(currentScale);
		image.setPosition(next.x, next.y);
		image.redraw();
	}
	
	
	public float getStartingScale() {
		return startingScale;
	}
	
	public float getMaxScale() {
		return maxScale;
	}

	public void setMaxScale(float maxScale) {
		this.maxScale = maxScale;
	}

	public float getMinScale() {
		return minScale;
	}

	public void setMinScale(float minScale) {
		this.minScale = minScale;
	}
	
	public float getDoubleTapScale() {
		return doubleTapScale;
	}
	
	public void setDoubleTapScale(float doubleTapScale) {
		this.doubleTapScale = doubleTapScale;
	}

	protected void boundCoordinates() {
		if(next.x < boundaryLeft) {
			next.x = boundaryLeft;
		}
		else if(next.x > boundaryRight) {
			next.x = boundaryRight;
		}

		if(next.y < boundaryTop) { 
			next.y = boundaryTop;
		}
		else if(next.y > boundaryBottom) {
			next.y = boundaryBottom;
		}
	}
	
	protected void calculateBoundaries() {
		int effectiveWidth = Math.round( (float) imageWidth * currentScale );
		int effectiveHeight = Math.round( (float) imageHeight * currentScale );
		
		canDragX = effectiveWidth > displayWidth;
		canDragY = effectiveHeight > displayHeight;
		
		if(canDragX) {
			float diff = (float)(effectiveWidth - displayWidth) / 2.0f;
			boundaryLeft = centerX - diff;
			boundaryRight = centerX + diff;
		}
		
		if(canDragY) {
			float diff = (float)(effectiveHeight - displayHeight) / 2.0f;
			boundaryTop = centerY - diff;
			boundaryBottom = centerY + diff;
		}
	}
	
	private boolean coordinatesOutsideDrawable(float x, float y) {
		Rect r = image.getDrawable().getBounds();
		return  y < (centerY + (r.top    * currentScale)) ||
				y > (centerY + (r.bottom * currentScale)) ||
				x < (centerX + (r.left   * currentScale)) ||
				x > (centerX + (r.right  * currentScale));
	}
}
