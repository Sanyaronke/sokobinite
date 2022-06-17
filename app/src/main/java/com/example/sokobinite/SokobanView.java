package com.example.sokobinite;

import android.view.View;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.util.AttributeSet;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.io.StringReader;

public class SokobanView extends View {
    private static final int TILE_SIZE = 60;
    private final Drawable sokoban;
    private final Drawable wall;
    private final Drawable crate;
    private final Drawable goal;
    private final Drawable floor;
    private int tiles_wide;
    private int tiles_high;
    private boolean tall;
    private int side_border_width;
    private int side_border_height;
    private int arena_x_lower_bound;
    private int arena_y_lower_bound;
    private int current_level;
    private final Point drag_start;
    private final Point drag_stop;
    private final MapList map_list;
    private Arena arena;
    private final PersistStoring store;

    public SokobanView(SokobanPlayability context) {
        super(context);
        store = new PersistStoring(context);
        floor = getResources().getDrawable(R.drawable.floor);
        sokoban = getResources().getDrawable(R.drawable.sokoban);
        wall = getResources().getDrawable(R.drawable.wall);
        crate = getResources().getDrawable(R.drawable.crate);
        goal = getResources().getDrawable(R.drawable.goal);
        map_list = new MapList(getResources().openRawResource(R.raw.sokoban));
        drag_start = new Point();
        drag_stop = new Point();
    }

    public SokobanView(SokobanPlayability context, int level) {
        this(context);
        current_level = level;
        loadGame();
    }

    public SokobanView(Context context, AttributeSet attributes) {
        this((SokobanPlayability) context);
    }

    public Arena getArena() { return arena; }
    public int getCurrentLevel() { return current_level; }

    public void retryLevel() {
        selectMap(current_level);
    }
    public void skipLevel() {
        nextLevel();
    }
    public void backToMenu() { // for debugging
        selectMap(11);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        updateStatusBar();
        int a_wid    = arena.getMapWidth();
        int a_height = arena.getMapHeight();
        Drawable tile;

        for(int x = 0; x < a_wid; x++) {
            for(int y = 0; y < a_height; y++) {
                tile = tileForLocation(x, y);
                int left, top, right, bottom;

                if(tall) {

                    int x_offset = (tiles_wide * TILE_SIZE - (a_height * TILE_SIZE))/2;
                    int y_offset = (tiles_high * TILE_SIZE - (a_wid * TILE_SIZE))/2;

                    left = arena_x_lower_bound + y * TILE_SIZE + x_offset;
                    top = arena_y_lower_bound + x * TILE_SIZE + y_offset;
                    right = arena_x_lower_bound + y * TILE_SIZE + TILE_SIZE + x_offset;
                    bottom = arena_y_lower_bound + x * TILE_SIZE + TILE_SIZE + y_offset;

                } else {

                    int x_offset = (tiles_wide * TILE_SIZE - (a_wid * TILE_SIZE))/2;
                    int y_offset = (tiles_high * TILE_SIZE - (a_height * TILE_SIZE))/2;

                    left = arena_x_lower_bound + y * TILE_SIZE + x_offset;
                    top = arena_y_lower_bound + x * TILE_SIZE + y_offset;
                    right = arena_x_lower_bound + y * TILE_SIZE + TILE_SIZE + x_offset;
                    bottom = arena_y_lower_bound + x * TILE_SIZE + TILE_SIZE + y_offset;
                }

                tile.setBounds(left, top, right, bottom);
                tile.draw(canvas);
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        tiles_wide = w / TILE_SIZE;
        tiles_high = h / TILE_SIZE;
        tall = tiles_high > tiles_wide;
        if (tall) {
            side_border_width = (tiles_wide - arena.getMapHeight()) / 2;
            side_border_height = (tiles_high - arena.getMapWidth()) / 2;
            arena_x_lower_bound = side_border_width;
            arena_y_lower_bound = side_border_height;
        } else {
            side_border_width = (tiles_wide - arena.getMapWidth()) / 2;
            side_border_height = (tiles_high - arena.getMapHeight()) / 2;
            arena_x_lower_bound = side_border_width;
            arena_y_lower_bound = side_border_height;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                drag_start.set((int) event.getX(), (int) event.getY());
                break;
            case MotionEvent.ACTION_UP:
                drag_stop.set((int) event.getX(), (int) event.getY());
                touchMove();
                break;
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Rect invalid;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                doMove(Arena.NORTH);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                doMove(Arena.SOUTH);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                doMove(Arena.EAST);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                doMove(Arena.WEST);
                break;
            default:
                return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    protected void touchMove() {
        int delta_x = drag_stop.x - drag_start.x;
        int delta_y = drag_stop.y - drag_start.y;
        if (Math.abs(delta_x) < 10 && Math.abs(delta_y) < 10) {
            return;
        }
        if(Math.abs(delta_x) > Math.abs(delta_y)) {
            if(delta_x < 0) {
                doMove(Arena.EAST);
            } else {
                doMove(Arena.WEST);
            }
        } else {
            if(delta_y < 0) {
                doMove(Arena.NORTH);
            } else {
                doMove(Arena.SOUTH);
            }
        }
    }

    protected void doMove(int direction) {
        Rect invalid;

        if (tall) {
            switch(direction) {
                case Arena.SOUTH: direction = Arena.EAST; break;
                case Arena.NORTH: direction = Arena.WEST; break;
                case Arena.EAST: direction = Arena.NORTH; break;
                case Arena.WEST: direction = Arena.SOUTH; break;
            }
        }

        arena.moveMan(direction);
        invalidate();
        updateStatusBar();

        if(arena.gameWon()) {
            levelWon();
            nextLevel();
        }
    }

    protected Drawable tileForLocation(int x, int y) {
        switch(arena.getTile(x,y)) {
            case Arena.FLOOR: return floor;
            case Arena.SOKOBAN: return sokoban;
            case Arena.WALL: return wall;
            case Arena.GOAL: return goal;
            case Arena.CRATE: return crate;
        }
        return floor;
    }


    protected void loadGame() {
        message("Loading game.");
        if (current_level == -1) {
            String saved_game = ((SokobanPlayability) getContext()).getSavedGame();
            if (saved_game == null) {
                message("No saved game found. Starting from first level.");
                current_level = 0;
                loadGame();
            } else {
                current_level = ((SokobanPlayability) getContext()).getSavedLevel();
                message("Saved game found for level #" + current_level);
                arena = new MapList(new StringReader(saved_game)).selectMap(0);
            }
        } else {
            message("Loading level #" + current_level);
            selectMap(current_level);
        }
    }

    protected void selectMap(int level) {
        arena = map_list.selectMap(level);
        Log.d("VIEW_SELECT_MAP", Integer.toString(arena.getMapWidth()));
        current_level = level;
        updateStatusBar();
        invalidate();
    }

    protected void levelWon() {
        store.addScore("main", current_level, arena.getMoves());
    }

    protected void nextLevel() {
        selectMap(current_level + 1);
    }

    protected void message(String message) {
        Log.d("SOKO", message);
    }

    protected void updateStatusBar() {
        ((SokobanPlayability) getContext()).setStatusBar(
                "Level: " + (current_level + 1) +
                        " | Moves: " + arena.getMoves()
        );
    }
}
