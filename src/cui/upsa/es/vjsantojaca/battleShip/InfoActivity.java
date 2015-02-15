package cui.upsa.es.vjsantojaca.battleShip;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class InfoActivity extends Activity 
{
	private Button continuar;
	private TextView textoInfo;
	private MediaPlayer mediaPlayer;;
	private OnClickListener btlistener = new View.OnClickListener()
	{
		@Override 
		public void onClick(View v) 
		{
			Intent intent = new Intent(InfoActivity.this, ColocarBarcoActivity.class);
			startActivity(intent);
			finish();
		}
	};
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        textoInfo = (TextView) findViewById(R.id.tv_info);
        continuar = (Button) findViewById(R.id.bt_continuar);
        textoInfo.setText("\t Bienvenido a NFC BATTLESHIP: Android vs Windows Phone 8. \n" +
        				" \t Si quieres disfrutar de un buen momento este es tu juego, la mecánica es fácil. " +
        				"colocas tus barcos en la tabla, cuando estén todos tus barcos y los de tu contrincante acercad los móviles" +
        				" y dad al botón GO!!, sigues las instrucciones que te vayan saliendo y... que empiece la diversión.");
        
        continuar.setOnClickListener(btlistener);

		mediaPlayer = MediaPlayer.create(this,R.raw.morse);
        mediaPlayer.setVolume(100,100);
        mediaPlayer.start();
        }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.info, menu);
        return true;
    }
    
}
