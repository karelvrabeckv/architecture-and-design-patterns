package cz.cvut.fit.miadp.mvcgame.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.LinkedBlockingQueue;

import cz.cvut.fit.miadp.mvcgame.abstractFactory.GameObjectFactory_A;
import cz.cvut.fit.miadp.mvcgame.abstractFactory.IGameObjectFactory;
import cz.cvut.fit.miadp.mvcgame.builder.Difficulty;
import cz.cvut.fit.miadp.mvcgame.builder.DifficultyBuilder;
import cz.cvut.fit.miadp.mvcgame.builder.Director;
import cz.cvut.fit.miadp.mvcgame.command.AbstractGameCommand;
import cz.cvut.fit.miadp.mvcgame.config.MvcGameConfig;
import cz.cvut.fit.miadp.mvcgame.model.gameObjects.*;
import cz.cvut.fit.miadp.mvcgame.observer.IObserver;

public class GameModel implements IGameModel {

    private Difficulty difficulty;
    private List<IObserver> observers;
    private IGameObjectFactory factory;

    private AbsCannon cannon;
    private List<AbsMissile> missiles;
    private List<AbsEnemy> enemies;
    private List<AbsObstacle> obstacles;
    private List<AbsBomb> bombs;
    private List<AbsCollision> collisions;
    private AbsGameInfo gameInfo;

    private Queue<AbstractGameCommand> unexecutedCommands;
    private Stack<AbstractGameCommand> executedCommands;

    public GameModel( ) {
        this.difficulty = createDifficulty( MvcGameConfig.EASY_DIFFICULTY );
        this.observers = new ArrayList<IObserver>( );
        this.factory = new GameObjectFactory_A( this );

        this.cannon = factory.createCannon( );
        this.missiles = new ArrayList<AbsMissile>( );
        this.enemies = createEnemies( difficulty.getNumOfEnemies( ) );
        this.obstacles = createObstacles( difficulty.getNumOfObstacles( ) );
        this.bombs = createBombs( difficulty.getNumOfBombs( ) );
        this.collisions = new ArrayList<AbsCollision>( );
        this.gameInfo = factory.createGameInfo( );

        this.unexecutedCommands = new LinkedBlockingQueue<AbstractGameCommand>( );
        this.executedCommands = new Stack<AbstractGameCommand>( );
    }

    private Difficulty createDifficulty( String difficulty ) {
        Director director = new Director( );
        DifficultyBuilder builder = new DifficultyBuilder( );

        switch ( difficulty ) {
            case MvcGameConfig.EASY_DIFFICULTY:
                director.constructEasyDifficulty( builder );
                break;
            case MvcGameConfig.NORMAL_DIFFICULTY:
                director.constructNormalDifficulty( builder );
                break;
            case MvcGameConfig.HARD_DIFFICULTY:
                director.constructHardDifficulty( builder );
                break;
        }

        return builder.getProduct( );
    }

    private List<AbsEnemy> createEnemies( int num ) {
        List<AbsEnemy> enemies = new ArrayList<AbsEnemy>( );

        for ( int i = 0; i < num; i++ ) {
            enemies.add( this.factory.createEnemy( ) );
        }

        return enemies;
    }

    private List<AbsObstacle> createObstacles( int num ) {
        List<AbsObstacle> obstacles = new ArrayList<AbsObstacle>( );

        for ( int i = 0; i < num; i++ ) {
            obstacles.add( this.factory.createObstacle( ) );
        }

        return obstacles;
    }

    private List<AbsBomb> createBombs( int num ) {
        List<AbsBomb> bombs = new ArrayList<AbsBomb>( );

        for ( int i = 0; i < num; i++ ) {
            bombs.add( this.factory.createBomb( ) );
        }

        return bombs;
    }

    public void changeDifficulty( String difficulty ) {
        this.difficulty = createDifficulty( difficulty );
        restart( );
    }

    public void restart( ) {
        cannon = factory.createCannon( );
        missiles.clear( );
        enemies.clear( );
        obstacles.clear( );
        bombs.clear( );
        collisions.clear( );
        gameInfo = factory.createGameInfo( );

        unexecutedCommands.clear( );
        executedCommands.clear( );
    }

    public void update( ) {
        this.executedCommands( );
        this.moveMissiles( );
        this.destroyObjects( );
        this.destroyCollisions( );
        this.addObjects( );
        this.notifyObservers( );
    }

    private void executedCommands( ) {
        while( !this.unexecutedCommands.isEmpty( ) ){
            AbstractGameCommand cmd = this.unexecutedCommands.poll( );
            cmd.doExecute( );
            this.executedCommands.push( cmd );
        }
    }

    private void moveMissiles( ) {
        for ( AbsMissile missile : missiles ) {
            missile.move( );
        }
    }

    private void destroyObjects( ) {
        List<AbsMissile> missilesToRemove = new ArrayList<AbsMissile>( );
        List<AbsEnemy> enemiesToRemove = new ArrayList<AbsEnemy>( );
        List<AbsBomb> bombsToRemove = new ArrayList<AbsBomb>( );

        for ( AbsMissile missile : missiles ) {
            // check the bound x
            if ( missile.getPosition( ).getX( ) < 0 || missile.getPosition( ).getX( ) > MvcGameConfig.SCREEN_X ) {
                missilesToRemove.add( missile );
            }

            // check the bound y
            if ( missile.getPosition( ).getY( ) < 0 || missile.getPosition( ).getY( ) > MvcGameConfig.SCREEN_Y ) {
                missilesToRemove.add( missile );
            }

            // check the collisions with enemies
            for ( AbsEnemy enemy : enemies ) {
                if ( missile.getDistanceTo( enemy ) < MvcGameConfig.COLLISION_DISTANCE ) {
                    collisions.add( enemy.explode( ) ) ;
                    gameInfo.setScore( gameInfo.getScore() + 1 );
                    missilesToRemove.add( missile );
                    enemiesToRemove.add( enemy );
                }
            }

            // check the collisions with collisions
            for ( AbsCollision collision : collisions ) {
                if ( missile.getDistanceTo( collision ) < MvcGameConfig.COLLISION_DISTANCE ) {
                    missilesToRemove.add( missile );
                }
            }

            // check the collisions with obstacles
            for ( AbsObstacle obstacle : obstacles ) {
                if ( missile.getDistanceTo( obstacle ) < MvcGameConfig.COLLISION_DISTANCE ) {
                    missilesToRemove.add( missile );
                }
            }

            // check the collisions with bombs
            for ( AbsBomb bomb : bombs ) {
                if ( missile.getDistanceTo( bomb ) < MvcGameConfig.COLLISION_DISTANCE ) {
                    missilesToRemove.add( missile );
                    bombsToRemove.add( bomb );

                    for ( AbsEnemy enemy : enemies ) {
                        if ( bomb.getDistanceTo( enemy ) < MvcGameConfig.BOMB_DISTANCE ) {
                            collisions.add( enemy.explode( ) ) ;
                            gameInfo.setScore( gameInfo.getScore() + 1 );
                            enemiesToRemove.add( enemy );
                        }
                    }
                }
            }
        }

        missiles.removeAll( missilesToRemove );
        enemies.removeAll( enemiesToRemove );
        bombs.removeAll( bombsToRemove );
    }

    private void destroyCollisions( ) {
        List<AbsCollision> collisionsToRemove = new ArrayList<AbsCollision>( );

        for ( AbsCollision collision : collisions ) {
            if ( collision.getAge() > MvcGameConfig.COLLISION_AGE ) {
                collisionsToRemove.add( collision );
            }
        }

        collisions.removeAll( collisionsToRemove );
    }

    private void addObjects( ) {
        int numOfNewEnemies = difficulty.getNumOfEnemies( ) - enemies.size( ) - collisions.size( );
        List<AbsEnemy> newEnemies = createEnemies( numOfNewEnemies );
        enemies.addAll( newEnemies );

        int numOfNewObstacles = difficulty.getNumOfObstacles( ) - obstacles.size( );
        List<AbsObstacle> newObstacles = createObstacles( numOfNewObstacles );
        obstacles.addAll( newObstacles );

        int numOfNewBombs = difficulty.getNumOfBombs( ) - bombs.size( );
        List<AbsBomb> newBombs = createBombs( numOfNewBombs );
        bombs.addAll( newBombs );
    }

    public List<GameObject> getGameObjects( ) {
        List<GameObject> gameObjects = new ArrayList<GameObject>( );

        gameObjects.add( cannon );
        gameObjects.addAll( missiles );
        gameObjects.addAll( enemies );
        gameObjects.addAll( obstacles );
        gameObjects.addAll( bombs );
        gameObjects.addAll( collisions );
        gameObjects.add( gameInfo );

        return gameObjects;
    }

    public Difficulty getDifficulty( ) { return difficulty; }
    public AbsCannon getCannon( ) { return cannon; }
    public List<AbsMissile> getMissiles( ) {
        return this.missiles;
    }
    public List<AbsEnemy> getEnemies( ) {
        return this.enemies;
    }
    public List<AbsObstacle> getObstacles( ) {
        return this.obstacles;
    }
    public List<AbsBomb> getBombs( ) {
        return this.bombs;
    }
    public List<AbsCollision> getCollisions( ) {
        return this.collisions;
    }
    public AbsGameInfo getGameInfo( ) {
        return gameInfo;
    }

    public void toggleMovingStrategy( ) {
        this.gameInfo.toggleMovingStrategy( );
    }
    public void toggleShootingMode( ){
        this.cannon.toggleShootingMode( );
    }

    public void moveCannonLeft( ) {
        this.cannon.moveLeft( );

        this.notifyObservers( );
    }
    public void moveCannonRight( ) {
        this.cannon.moveRight( );

        this.notifyObservers( );
    }
    public void aimCannonUp( ) {
        this.cannon.aimUp( );

        this.notifyObservers( );
    }
    public void aimCannonDown( ) {
        this.cannon.aimDown( );

        this.notifyObservers( );
    }
    public void cannonPowerUp( ) {
        this.cannon.powerUp( );

        this.notifyObservers( );
    }
    public void cannonPowerDown( ) {
        this.cannon.powerDown( );

        this.notifyObservers( );
    }
    public void cannonShoot( ) {
        List<AbsMissile> newMissiles = cannon.shoot( );
        int total = this.gameInfo.getUsedMissiles() + newMissiles.size( );

        if ( total <= difficulty.getTotalMissiles( ) ) {
            this.missiles.addAll( newMissiles ) ;
            this.gameInfo.setUsedMissiles( total );

            this.notifyObservers( );
        }
    }

    @Override
    public void registerObserver( IObserver obs ) {
        if( !this.observers.contains( obs ) ) {
            this.observers.add( obs );
        }
    }
    @Override
    public void unregisterObserver( IObserver obs ) {
        if( this.observers.contains( obs ) ) {
            this.observers.remove( obs );
        }
    }
    @Override
    public void notifyObservers( ) {
        for( IObserver obs : this.observers ){
            obs.update( );
        }
    }

    private class Memento {
        private int score;
        private int cannonX;
        private int cannonY;
        // GO positions
    }

    public Object createMemento( ) {
        Memento m = new Memento( );
        m.score = this.gameInfo.getScore();
        m.cannonX = this.getCannon( ).getPosition( ).getX( );
        m.cannonY = this.getCannon( ).getPosition( ).getY( );
        return m;
    }

    public void setMemento( Object memento ) {
        Memento m = ( Memento ) memento;
        this.gameInfo.setScore( m.score );
        this.cannon.getPosition( ).setX( m.cannonX );
        this.cannon.getPosition( ).setY( m.cannonY );
    }

    @Override
    public void registerCommand( AbstractGameCommand cmd ) {
        this.unexecutedCommands.add( cmd );
    }

    @Override
    public void undoLastCommand( ) {
        if( !this.executedCommands.isEmpty( ) ){
            AbstractGameCommand cmd = this.executedCommands.pop( );
            cmd.unExecute( );
        }
        this.notifyObservers( );
    }

}
