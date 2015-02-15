package cui.upsa.es.vjsantojaca.battleShip;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class FinActivity extends Activity 
{
	private boolean ganado;
	private Button salir;
	private Button volverAJugar;
	private TextView textG;
	private Context context = FinActivity.this;
	private View.OnClickListener listener = new View.OnClickListener() 
	{	
		@Override
		public void onClick(View v) 
		{
			
			switch( v.getId())
			{
				case R.id.bt_volver:
					Intent intent = new Intent(FinActivity.this, InfoActivity.class);
					startActivity(intent);
					finish();
					break;
				case R.id.bt_salir:
					finish();
					break;
				default:
					break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fin);
		textG = (TextView) findViewById(R.id.tvFin);
		volverAJugar = (Button) findViewById(R.id.bt_volver);
		salir = (Button) findViewById(R.id.bt_salir);
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if ( extras != null)
		{
			ganado = (Boolean) extras.get("ganado");
			if( ganado == true )
			{
				textG.setText("HAS GANADO");
			}
			else
			{
				textG.setText("HAS PERDIDO");
			}
			
			volverAJugar.setOnClickListener(listener);
			salir.setOnClickListener(listener );
		}
		else
		{
			Toast.makeText(context, "Ha habido un problema, lo sentimos.", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.fin, menu);
		return true;
	}

}
