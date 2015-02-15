package cui.upsa.es.vjsantojaca.battleShip;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import android.media.MediaPlayer;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class JuegoActivity extends Activity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback
{
	private static final int REQUEST_CODE_JUEGO = 2;
	private static final int INFORMACION_ENVIADO = 1;
	private static final int COORDENADAS_ENVIADO = 2;
	private MediaPlayer mediaPlayer;
	private TextView tvHundidos;
	private String mensajeEnviar;
	private NfcAdapter nfc_adapter;
	private TableLayout table;
	private boolean ganado;
	private int envio; //Si es 1 es informacion, si es 2 es coordenadas
	private int hundidosMiMapa = 0;
	private int hundidosSuMapa = 0;
	private PendingIntent pendingIntent;
	private int[][] mapaBarcos = new int[10][10];
	int[] linea;
	private int res_agua;
	private int res_tocado;
	private int fila;
	private int columna;
	private int[][] mapaBarcosContrincante = new int[10][10];
	private Context context = JuegoActivity.this;
	private int turno;
	
	private OnClickListener btlistener = new View.OnClickListener()
	{
		@Override
		public void onClick(View v) 
		{
			switch( v.getId())
			{
			case R.id.tblBoardJuego:
				for (int row = 0; row < table.getChildCount(); row++)
				{
					View view = table.getChildAt(row);
					if (view instanceof TableRow)
					{
						TableRow tableRow = (TableRow) table.getChildAt(row);
						for (int col = 0; col < tableRow.getChildCount(); col++)
						{
							if (tableRow.getChildAt(col) instanceof ImageView)
							{
								View cellView = tableRow.getChildAt(col);
								cellView.setOnClickListener(new ImageViewClickListener((row), col));
							}
						}
					}
				}
				break;
			}
		}
	};
	
	private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case INFORMACION_ENVIADO:
            	//Todo lo que has enviado, depende lo que hayas enviado pasará una cosa u otra
            	envio = -1;
            	if ( mensajeEnviar.contains("Agua") )
				{
					turno = 1;
	       			Toast.makeText(context, "Es tu turno!!!!!", Toast.LENGTH_LONG).show();
				}
				
				else if ( mensajeEnviar.contains("Tocado") || mensajeEnviar.contains("Hundido") )
				{
					turno = 0;
	       			Toast.makeText(context, "No es su turno!!!!!", Toast.LENGTH_LONG).show();
				}
				
				else if ( mensajeEnviar.contains("HAS GANADO") )
				{
					ganado = false;
					
					Intent intentFin = new Intent(context , FinActivity.class);
					intentFin.putExtra("ganado",ganado);
					startActivityForResult(intentFin, REQUEST_CODE_JUEGO);
					finish();
				}
                break;

            case COORDENADAS_ENVIADO:
            	envio = -1;
            	break;
            }
        }
    };
    
	{
		for(int i = 0; i < 10; i++)
		{
			for(int j = 0; j < 10; j++)
			{
				mapaBarcosContrincante[i][j] = 0; 
				//Todo a cero es inicial
				//El 1 es agua
				//El 2 es tocado
			}
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_juego);
		res_agua = getResources().getIdentifier("tileagua", "drawable", getPackageName());
		res_tocado = getResources().getIdentifier("tiledado", "drawable", getPackageName());
		table = (TableLayout) findViewById(R.id.tblBoardJuego);
		tvHundidos = (TextView) findViewById(R.id.tvHundidos);
		
		tvHundidos.setText("Te quedan " + (10 - hundidosSuMapa) + "barcos por hundir, has hundido ya " + hundidosSuMapa + "barcos.");
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		
		if ( extras != null)
		{
			for(int i=0; i < 10; ++i)
			{
			  linea = intent.getIntArrayExtra("mapaBarcos" + i);
			  System.arraycopy(linea, 0, mapaBarcos[i], 0, linea.length);
			}
			turno = (Integer) extras.get("turno");
			
			Toast.makeText(context, "Cada vez que vaya a elegir una posición acerque el teléfono al de su " +
					"contrincante, por favor. Haga lo mismo cuando elija la posición su contrincante.", Toast.LENGTH_LONG).show();
			
			//Esto es para la primera vez que se ejecuta la actividad.
			if( turno == 0)
			{
				Toast.makeText(context, "Tiene que esperar, no es su turno.", Toast.LENGTH_LONG).show();
			}
			else if ( turno == 1 )
			{
				Toast.makeText(context, "Es su turno.", Toast.LENGTH_LONG).show();
			}
			
			
			nfc_adapter = NfcAdapter.getDefaultAdapter(context);
			
			if (nfc_adapter == null) {
	            Toast.makeText(context, "NFC no está permitido, la aplicación se cerrará", Toast.LENGTH_LONG).show();
	            finish();
	            return;
	        }
			
			pendingIntent = PendingIntent.getActivity(
	                this, 0, new Intent( context, getClass() ).addFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP ), 0
	        );
			
			table.setOnClickListener(btlistener);
			
		}
		else
		{
			Toast.makeText(context, "Lo sentimos, pero se ha producido un error" +
					" vuelva a empezar el juego y comprueba que tiene bien conectado el sistema NFC", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.juego, menu);
		return true;
	}
	
	@Override
	protected void onResume() 
	{
		super.onResume();
		
		if( nfc_adapter != null )
        {
            if( nfc_adapter.isEnabled() )
            {
            	nfc_adapter.enableForegroundDispatch( this, pendingIntent, null, null );
            }
        }
	}

	
	
	@Override
	protected void onPause() 
	{
		super.onPause();
		nfc_adapter.disableForegroundDispatch( this );
	}
	
	@Override
	public void onNewIntent( Intent intent )
    {
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) 
    	{
			ReadSendNfc rsn = new ReadSendNfc();
			String resultado;
			if ((resultado = rsn.recibirInformacion(intent)) != null)
			{
				Log.d( "NfcRecibido", "Resultado: " + resultado ); 
				//Ahora pueden existir dos opciones, una que hayamos recibido el resultado de nuestro envío de coordenadas
				//o que hayamos recibido las coordenadas para comprobar el resultado
			
				if(!resultado.equals("coordenadas"))
				{
					TableRow tableRow = (TableRow) table.getChildAt(fila + 1);
					View cellView = tableRow.getChildAt(columna + 1);
					//Significa que ha devuelto la información sobre esa posición
					if ( resultado.contains("Agua") )
					{
						//Lo ponemos azul

	            		mediaPlayer = MediaPlayer.create(this,R.raw.agua);
	                    mediaPlayer.setVolume(100,100);
	                    mediaPlayer.start();
	                    Toast.makeText(context, "Ha sido agua.", Toast.LENGTH_LONG).show();
						((ImageView) cellView).setImageResource(res_agua);
						turno = 0;
					}

					else if ( resultado.contains("Tocado") || resultado.contains("Hundido") )
					{
						if( resultado.contains("Tocado"))
						{
							Toast.makeText(context, "Ha sido tocado.", Toast.LENGTH_LONG).show();
						}
						else
						{
							hundidosSuMapa ++;
							tvHundidos.setText("Te quedan " + (10 - hundidosSuMapa) + "barcos por hundir, has hundido ya " + hundidosSuMapa + "barcos.");
							Toast.makeText(context, "Has hundido un barco.", Toast.LENGTH_LONG).show();
				            
						}
						//Lo ponemos rojo
						mediaPlayer = MediaPlayer.create(this,R.raw.tocado);
	                    mediaPlayer.setVolume(100,100);
	                    mediaPlayer.start();
						((ImageView) cellView).setImageResource(res_tocado);
						turno = 1;
					}

					else if ( resultado.contains("HAS GANADO") )
					{
						//Esto significaría que ya has ganado
						//Enviaríamos a una nueva activity y cerraríamos esta.
						ganado = true;

						Intent intentFin = new Intent(context , FinActivity.class);
						intentFin.putExtra("ganado",ganado);
						startActivityForResult(intentFin, REQUEST_CODE_JUEGO);
						finish();
					}	
				}
			}

    	}
    }
	
	public class ReadSendNfc
    {
		Intent intent;
		
        public ReadSendNfc() {}

		public String recibirInformacion(Intent intent) 
        {
			this.intent = intent;
       		Log.d( "NfcRead", "Action: " + intent.getAction() );
    		
        	Parcelable[] rMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        	NdefMessage msg = (NdefMessage) rMsg[0]; //Tomamos solo el primero proque sabemos que solo es un mensaje.
        	//Ahora vamos a suponer que se recibe en el mensaje (como somos nosotros también quien lo envíamos)
        	//O nos han enviado dos records (primero las x y luego las y)
        	//O nos ha mandado 1 solo record
        
        	NdefRecord[] ndrs = msg.getRecords(); //pillamos todos los record
        	
        	if ( ndrs.length > 1)
        	{
        		final NdefRecord ndrX = ndrs[0];
            	final NdefRecord ndrY = ndrs[1];
            	
            	try 
            	{
            		String textEncoding = "UTF-8";
            		int languageCodeLength = "es".getBytes().length;
            
           
            		int x = Integer.parseInt(new String(ndrX.getPayload() , 
            				languageCodeLength + 1 , 
            				ndrX.getPayload().length - languageCodeLength - 1 ,
            				textEncoding));
    			
            		int y = Integer.parseInt(new String(ndrY.getPayload() , 
            				languageCodeLength + 1 , 
            				ndrY.getPayload().length - languageCodeLength - 1 ,
            				textEncoding));

                    Log.d( "NfcRead", "Info: " + x + "  " + y ); 
            		enviarResultado(x, y);
            		return ("coordenadas");
            		
            	} catch (NumberFormatException e) 
            	{
            		finish();
            	} catch (UnsupportedEncodingException e)
            	{
            		Toast.makeText(context, "Lo sentimos, pero se ha producido un error" +
            				" ,algo no funcionó en la codificación.", Toast.LENGTH_LONG).show();
    				finish();
            	}
        	}
        	else
        	{
        		String resultado = null; 

            	final NdefRecord ndrResult = ndrs[0];
            	
            	try 
            	{
            		String textEncoding = "UTF-8";
            		int languageCodeLength = "es".getBytes().length;
            
           
            		resultado = new String(ndrResult.getPayload() , 
            				languageCodeLength + 1 , 
            				ndrResult.getPayload().length - languageCodeLength - 1 ,
            				textEncoding);
            		
            		return resultado;
            	} catch (UnsupportedEncodingException e)
            	{
            		Toast.makeText(context, "Lo sentimos, pero se ha producido un error" +
            				" ,algo no funcionó en la codificación.", Toast.LENGTH_LONG).show();
    				finish();
            	}
        	}
        	return null;
        	
        	
		}

       	public void enviarResultado(int x, int y) 
       	{
       		//Ahora comprobamos la matriz para ver que hay en la posición x, y
       		if( mapaBarcos[x][y] == 0)
       		{
       			//Ha dado en agua, así que habrá que envíar un mensaje al contrincante diciendo que ha sido agua.

       			send("Agua");
       		}
       		else if( mapaBarcos[x][y] == 1)
       		{
       			//Ha sido tocado, habrá que comprobar si ha sido hundido o simplemente ha sido tocado.

       			//Recorremos la fila
       			for( int i = 0; i < (10 - y); i ++)
       			{
       				if( mapaBarcos[x][y + i] == 1 && i > 0)
       				{
       					//Es que por ese lado hay más barco, se enviaría un tocado.

       	       			mapaBarcos[x][y] = 2;

       					send("Tocado");
       					break;
       				}
       				else if( mapaBarcos[x][y + i] == 0 && i > 0 || mapaBarcos[x][y + i] == 1 && (y + i) == 9)
       				{
       					//Es que por ese lado no hay más barco, hay que buscar por el otro lado
       					for( int j = y; j > -1; j --)
       					{
       						if( mapaBarcos[x][j] == 1 && j!=y)
       						{
       							//Es que por ese lado hay más barco, se enviaría un tocado.

       			       			mapaBarcos[x][y] = 2;

       							send("Tocado");
       							break;
       						}
       						else if (mapaBarcos[x][j] == 0 && j!=y || mapaBarcos[x][j] == 1 && j == 0)
       						{
       							//Puede que esté hundido o que simplemente el barco esté en forma vertical
       							
       							for (int z = x; z < 10;  z++ ) //primero vamos para abajo
       							{
       								if( mapaBarcos[z][y] == 1 && z != x)
       								{
       									//TOCADO

       					       			mapaBarcos[x][y] = 2;

       									send("Tocado");
       									break;
       								}
       								else if ( mapaBarcos[z][y] == 0 && z != x || mapaBarcos[z][y] == 1 && z == 9)
       								{
       									//Hay que comprobar para arriba
       									for(int k = x; k > -1; k--)
       									{
       										if( mapaBarcos[k][y] == 1 && k != x)
       										{
       											//TOCADO

       							       			mapaBarcos[x][y] = 2;

       											send("Tocado");
       											break;
       										}
       										else if( mapaBarcos[k][y] == 0 && k != x || mapaBarcos[k][y] == 1 && k == 0 )
       										{
       											//Es que por este lado ya no hay más barco y se enviará un tocado y hundido.
       											
       											hundidosMiMapa ++;
       											
       											if( hundidosMiMapa == 10)
       											{
       												
       												Toast.makeText(context, "Su contrincante ha hundido el último de sus barcos" +
           													" ,HA PERDIDO!!.", Toast.LENGTH_LONG).show();
       												Toast.makeText(context, "Acerque el dispositivo al de su contrincante"
           													, Toast.LENGTH_LONG).show();

       								       			mapaBarcos[x][y] = 2;
       												
       												send("HAS GANADO"); //Has perdido y envías que el otro ha ganado
       												break;	
       											}
       											else
       											{
       								       			mapaBarcos[x][y] = 2;
           											send("Hundido");
       											}
       											break;
       										}
       									}
       									break;
       								}
       							}
       							break;
       						}
       					}
       					break;
       				}
       			}
       		}
       	}
    
        public void send(final String mensaje)
        {
        	String mensajeDialogo = "Vamos a enviar el resultado a su oponente, dé a continuar y acerque el móvil a su contrincante.";
			AlertDialog.Builder builder = new AlertDialog.Builder(context);
			builder.setMessage(mensajeDialogo)
			        .setTitle("Advertencia")
			        .setCancelable(false)
			        .setPositiveButton("Continuar",
			                new DialogInterface.OnClickListener() {
			                    public void onClick(DialogInterface dialog, int id) 
			                    {
			                    	envio = 1;
			                    	mensajeEnviar = mensaje;
			                    	nfc_adapter.setNdefPushMessageCallback(JuegoActivity.this, JuegoActivity.this);
			    					nfc_adapter.setOnNdefPushCompleteCallback(JuegoActivity.this, JuegoActivity.this);
			            		}
			                });
			AlertDialog alert = builder.create();
			alert.show();
        	
        	
        }
    }
    
    class ImageViewClickListener implements View.OnClickListener
	{
		private int row = -1;
		private int col = -1;

		public ImageViewClickListener(int row, int column)
		{
			this.row = row - 1;
			this.col = column - 1;
		}


		public void onClick(View v)
		{
			if(v instanceof ImageView)
			{
				if( turno == 1)
				{
					String mensaje = "¿Su decisión son las coordenadas [" + row + "," + col + "]?";
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setMessage(mensaje)
					        .setTitle("Advertencia")
					        .setCancelable(false)
					        .setNegativeButton("Cancelar",
					                new DialogInterface.OnClickListener() {
					                    public void onClick(DialogInterface dialog, int id) 
					                    {
					                        dialog.cancel();
					                    }
					                })
					        .setPositiveButton("Continuar",
					                new DialogInterface.OnClickListener() {
					                    public void onClick(DialogInterface dialog, int id) 
					                    {
					                    	Toast.makeText(context, "Mantenga cerca de su contrincante el teléfono.", Toast.LENGTH_LONG).show();
					    					if( mapaBarcosContrincante[row][col] != 2 && mapaBarcosContrincante[row][col] != 1)
					    					{
					    						fila = row;
					    						columna = col;
					    						envio = 2;
					    						nfc_adapter.setNdefPushMessageCallback(JuegoActivity.this, JuegoActivity.this);
						    					nfc_adapter.setOnNdefPushCompleteCallback(JuegoActivity.this, JuegoActivity.this);
					    					}
					    					else
					    					{
					    						Toast.makeText(context, "Ha dado a una posición que ya se ha dicho" +
					    								" ,elija otra posición.", Toast.LENGTH_LONG).show();
					    					}
					                    }
					                });
					AlertDialog alert = builder.create();
					alert.show();
				}
				else
				{
					Toast.makeText(context, "Lo sentimos, aún no es su turno. Espere a que su contrincante" +
							" haga la selección.", Toast.LENGTH_LONG).show();
				}
			}
		}
	}

	@Override
	public void onNdefPushComplete(NfcEvent event) 
	{
		if(envio == 1)
		{
			handler.obtainMessage(INFORMACION_ENVIADO).sendToTarget(); //confirmación de información
		}
		else
		{
			handler.obtainMessage(COORDENADAS_ENVIADO).sendToTarget(); //confirmación de coordenadas
		}
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) 
	{
		NdefMessage ndefMessage = null;
		
		if (envio == 1)
		{
			Log.d( "NfcSend", "Mensaje: " + mensajeEnviar ); 
			String lang = "es";
			Charset utfEncoding = Charset.forName("UTF-8");
			byte[] textBytes = mensajeEnviar.getBytes(utfEncoding);
			byte[] langBytes = lang.getBytes(utfEncoding);
			int langLength = langBytes.length;
			int textLength = textBytes.length;
			byte[] payLoad = new byte[1 + langLength + textLength];


			payLoad[0] = (byte) langLength;

			System.arraycopy(langBytes, 0, payLoad, 1, langLength);
			System.arraycopy(textBytes, 0, payLoad, 1 + langLength, textLength);

			NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, 
					NdefRecord.RTD_TEXT, 
					new byte[0], payLoad);

			ndefMessage = new NdefMessage(record);
		}
		else
		{
			Log.d( "NFCSend", "Coordenadas: " + fila + columna ); 
			String x = Integer.toString(fila);
			String y = Integer.toString(columna);

			String lang = "es";
			Charset utfEncoding = Charset.forName("UTF-8");
			byte[] textBytesFil = x.getBytes(utfEncoding);
			byte[] textBytesCol = y.getBytes(utfEncoding);
			byte[] langBytes = lang.getBytes();
			int langLength = langBytes.length;
			int textLengthFil = textBytesFil.length;
			int textLengthCol = textBytesCol.length;

			byte[] payLoadFil = new byte[1 + langLength + textLengthFil];
			byte[] payLoadCol = new byte[1 + langLength + textLengthCol];


			payLoadFil[0] = (byte) langLength;
			payLoadCol[0] = (byte) langLength;

			System.arraycopy(langBytes, 0, payLoadFil, 1, langLength);
			System.arraycopy(textBytesFil, 0, payLoadFil, 1 + langLength, textLengthFil);

			System.arraycopy(langBytes, 0, payLoadCol, 1, langLength);
			System.arraycopy(textBytesCol, 0, payLoadCol, 1 + langLength, textLengthCol);

			NdefRecord recordX = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, 
					NdefRecord.RTD_TEXT, 
					new byte[0], payLoadFil);

			NdefRecord recordY = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, 
					NdefRecord.RTD_TEXT, 
					new byte[0], payLoadCol);

			ndefMessage = new NdefMessage(recordX, recordY);
		}
		return ndefMessage;
	}
}
