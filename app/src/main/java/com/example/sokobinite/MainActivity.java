package com.example.sokobinite;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends Activity implements OnClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game_option);

        findViewById(R.id.home_new_button).setOnClickListener(this);
        findViewById(R.id.home_continue_button).setOnClickListener(this);
        findViewById(R.id.home_high_score_button).setOnClickListener(this);
    }

    public void onClick(View v) {
        Intent game = new Intent(this, SokobanPlayability.class);
        Intent scores = new Intent(this, PlayerScore.class);
        switch(v.getId()) {
            case R.id.home_new_button:
                game.putExtra(SokobanPlayability.KEY_LEVEL, 0);
                startActivity(game);
                break;
            case R.id.home_continue_button:
                game.putExtra(SokobanPlayability.KEY_LEVEL, -1);
                startActivity(game);
                break;
            case R.id.home_high_score_button:
                startActivity(scores);
        }
    }
}