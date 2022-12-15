package cz.cvut.fit.miadp.mvcgame.model.gameObjects;

import cz.cvut.fit.miadp.mvcgame.model.IGameModel;
import cz.cvut.fit.miadp.mvcgame.model.Position;
import cz.cvut.fit.miadp.mvcgame.state.IShootingMode;
import cz.cvut.fit.miadp.mvcgame.strategy.IMovingStrategy;
import cz.cvut.fit.miadp.mvcgame.strategy.RealisticMovingStrategy;
import cz.cvut.fit.miadp.mvcgame.strategy.SimpleMovingStrategy;
import cz.cvut.fit.miadp.mvcgame.visitor.IVisitor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public abstract class AbsGameInfo extends GameObject {

    protected IGameModel model;
    protected LocalDateTime start;
    protected int score;
    protected int usedMissiles;
    protected IMovingStrategy movingStrategy;
    protected static IMovingStrategy SIMPLE_MOVING_STRATEGY = new SimpleMovingStrategy( );
    protected static IMovingStrategy REALISTIC_MOVING_STRATEGY = new RealisticMovingStrategy( );

    public abstract Position offsetPosition( int multiplier );

    public long getTime( ) {
        return ChronoUnit.MILLIS.between(start, LocalDateTime.now( )) / 1000;
    }
    public int getScore( ) {
        return score;
    }
    public void setScore( int score ) {
        this.score = score;
    }
    public int getUsedMissiles( ) {
        return usedMissiles;
    }
    public void setUsedMissiles( int usedMissiles ) {
        this.usedMissiles = usedMissiles;
    }

    public IShootingMode getShootingMode( ) {
        return model.getCannon( ).getShootingMode( );
    }
    public int getCannonAngle( ) {
        return Math.abs( ( int ) Math.toDegrees( model.getCannon( ).getAngle( ) ) );
    }
    public int getCannonPower( ) {
        return model.getCannon( ).getPower( );
    }

    public int getActiveMissiles( ) {
        return model.getMissiles( ).size( );
    }
    public int getActiveEnemies( ) {
        return model.getEnemies( ).size( );
    }
    public int getActiveCollisions( ) {
        return model.getCollisions( ).size( );
    }

    public IMovingStrategy getMovingStrategy( ) {
        return movingStrategy;
    }

    public void toggleMovingStrategy( ) {
        if ( movingStrategy instanceof SimpleMovingStrategy) {
            movingStrategy = REALISTIC_MOVING_STRATEGY;
        }
        else if ( movingStrategy instanceof RealisticMovingStrategy ){
            movingStrategy = SIMPLE_MOVING_STRATEGY;
        }
        else {

        }
    }

    @Override
    public void acceptVisitor( IVisitor visitor ) {
        visitor.visitGameInfo( this );
    }

}
