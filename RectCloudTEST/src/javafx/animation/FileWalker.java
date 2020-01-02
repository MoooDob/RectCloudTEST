package javafx.animation;

import javafx.animation.Animation;
import javafx.util.Duration;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;


public final class FileWalker extends Animation {
	
    private static final EventHandler<ActionEvent> DEFAULT_ON_STOPPED = null;
    private static final EventHandler<ActionEvent> DEFAULT_ON_STARTED = null;
    private static final EventHandler<ActionEvent> DEFAULT_ON_ACTION = null;
    private ObjectProperty<EventHandler<ActionEvent>> onStopped;
    private ObjectProperty<EventHandler<ActionEvent>> onStarted;
    
    private ObjectProperty<EventHandler<ActionEvent>> action;
	private EventHandler<ActionEvent> cachedAction;

    private ObjectProperty<Duration> duration;
    private static final Duration DEFAULT_DURATION = Duration.millis(400);

	@Override
	void doPlayTo(long currentTicks, long cycleTicks) {
		System.out.println("doPlayTo currentTicks " + currentTicks + " cycleTicks " + cycleTicks);
	}

	@Override
	void doJumpTo(long currentTicks, long cycleTicks, boolean forceJump) {
		System.out.println("doJumpTo currentTicks " + currentTicks + " cycleTicks " + cycleTicks + " forceJump " + forceJump);
	}
	
    @Override
    void doStart(boolean forceSync) {
		System.out.println("doStart forceSync " + forceSync);
        super.doStart(forceSync);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
		System.out.println("stop");
        if (parent != null) {
            throw new IllegalStateException("Cannot stop when embedded in another animation");
        }
        if (getStatus() == Status.RUNNING) {
        	getOnStopped();
        }
        super.stop();
    }
    
    public final EventHandler<ActionEvent> getOnStopped() {
		System.out.println("getOnStopped");
        return (onStopped == null)? DEFAULT_ON_STOPPED : onStopped.get();
    }
    
    public final EventHandler<ActionEvent> getOnStarted() {
		System.out.println("getOnStarted");
        return (onStarted == null)? DEFAULT_ON_STARTED: onStarted.get();
    }
    
    public final EventHandler<ActionEvent> getAction() {
		System.out.println("getOnAction");
        return (action == null)? DEFAULT_ON_ACTION : action.get();
    }
    

	@Override
	void startReceiver(long delay) {
		System.out.println("startReceiver");
		super.startReceiver(delay);
	}

	@Override
	void pauseReceiver() {
		System.out.println("pauseReceiver");
		super.pauseReceiver();
	}
	
	@Override
	void resumeReceiver() {
		System.out.println("resumeReceiver");
		super.resumeReceiver();
	}

	@Override
	public void jumpTo(Duration time) {
		System.out.println("jumpTo time " + time);
		super.jumpTo(time);
	}

	@Override
	public void jumpTo(String cuePoint) {
		System.out.println("jumpTo cuePoint " + cuePoint);
		super.jumpTo(cuePoint);
	}

	@Override
	public void playFrom(String cuePoint) {
		System.out.println("playFrom cuePoint " + cuePoint);
		super.playFrom(cuePoint);
	}

	@Override
	public void playFrom(Duration time) {
		System.out.println("playFrom time " + time);
		super.playFrom(time);
	}

	@Override
	public void play() {
		System.out.println("play");
		super.play();
	}

	@Override
	public void playFromStart() {
		System.out.println("playFromStart");
		super.playFromStart();
	}

	@Override
	public void pause() {
		System.out.println("pause");
		super.pause();
	}

	@Override
	boolean startable(boolean forceSync) {
		System.out.println("startable forceSync " + forceSync);	
        return super.startable(forceSync)
                && (getAction() != null);
	}

	@Override
	void sync(boolean forceSync) {
		System.out.println("sync forceSync " + forceSync);
        super.sync(forceSync);
        if (forceSync || (cachedAction == null)) {
            cachedAction = getAction();
        }
	}
	

	@Override
	void doPause() {
		System.out.println("doPause");
		super.doPause();
	}

	@Override
	void doResume() {
		System.out.println("doResume");
		super.doResume();
	}

	@Override
	void doStop() {
		System.out.println("doStop");
		super.doStop();
	}

	@Override
	void doTimePulse(long elapsedTime) {
		System.out.println("doTimePulse elapsedTime " + elapsedTime);
		super.doTimePulse(elapsedTime);
	}

//	@Override
//	void setCurrentTicks(long ticks) {
//		System.out.println("setCurrentTicks ticks" + ticks);
//		super.setCurrentTicks(ticks);
//	}

//	@Override
//	void setCurrentRate(double currentRate) {
//		System.out.println("setCurrentRate currentRate" + currentRate);
//		super.setCurrentRate(currentRate);
//	}

	public FileWalker() {
		super();
		System.out.println("construct FileWalker");
	}

	public FileWalker(double targetFramerate) {
		super(targetFramerate);
		System.out.println("construct FileWalker targetFramerate" + targetFramerate);
	}

    public final void setOnStopped(EventHandler<ActionEvent> value) {
		System.out.println("setOnStopped");
        if ((onStopped != null) || (value != DEFAULT_ON_STOPPED)) {
            onStoppedProperty().set(value);
        }
    }

    public final void setOnStarted(EventHandler<ActionEvent> value) {
		System.out.println("setOnStarted");
        if ((onStarted != null) || (value != DEFAULT_ON_STARTED )) {
            onStartedProperty().set(value);
        }
    }
    
    public final void setAction(EventHandler<ActionEvent> value) {
		System.out.println("setOnAction");
        if ((action != null) || (value != DEFAULT_ON_ACTION )) {
            onActionProperty().set(value);
        }
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onStoppedProperty() {
    	System.out.println("onStoppedProperty");
        if (onStopped == null) {
        	onStopped = new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onStopped", DEFAULT_ON_STOPPED);
        }
        return onStopped;
    }
    
    public final ObjectProperty<EventHandler<ActionEvent>> onStartedProperty() {
    	System.out.println("onStartedProperty");
        if (onStarted == null) {
        	onStarted = new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onStarted", DEFAULT_ON_STARTED);
        }
        return onStarted;
    }
    
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
    	System.out.println("onActionProperty");
        if (action == null) {
        	action = new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onAction", DEFAULT_ON_ACTION);
        }
        return action;
    }
    
    public final ObjectProperty<Duration> durationProperty() {
        if (duration == null) {
            duration = new ObjectPropertyBase<Duration>(DEFAULT_DURATION) {

                @Override
                public void invalidated() {
                    try {
                        setCycleDuration(getDuration());
                    } catch (IllegalArgumentException e) {
                        if (isBound()) {
                            unbind();
                        }
                        set(getCycleDuration());
                        throw e;
                    }
                }

                @Override
                public Object getBean() {
                    return FileWalker.this;
                }

                @Override
                public String getName() {
                    return "duration";
                }
            };
        }
        return duration;
    }
    
    public final Duration getDuration() {
        return (duration == null)? DEFAULT_DURATION : duration.get();
    }
    
    public final void setDuration(Duration value) {
        if ((duration != null) || (!DEFAULT_DURATION.equals(value))) {
            durationProperty().set(value);
        }
    }

}
