package cui.upsa.es.vjsantojaca.battleShip;

import java.nio.charset.Charset;
import java.util.Random;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class ColocarBarcoActivity extends Activity implements CreateNdefMessageCallback, OnNdefPushCompleteCallback
{
	protected static final String TAG = ColocarBarcoActivity.class.getSimpleName();
	private static final int REQUEST_CODE_COLOCAR = 1;
	private static final int MENSAJE_ENVIADO = 1;
	private int res_barco;
	private int valorRecibido;
	private int numeroAleatorio;
	private boolean recibido;
	private boolean enviado;
	private PendingIntent pendingIntent;
	private NfcAdapter nfc_adapter;
	private int[][] mapaBarcos = new int[10][10];
	private int[] numBarcos = new int[4];
	private Button go;
	private Spinner spiBarcos;
	private Spinner spiOrientacion;
	private TextView tvNumBarcos;
	private Context context = ColocarBarcoActivity.this;
	private TableLayout table;
	private boolean procesingNFC=false;
	
	private OnClickListener btlistener = new View.OnClickListener() 
	{
		@Override
		public void onClick(View v) 
		{
			switch( v.getId())
			{
			case R.id.bt_go:
				int total = 0;
				//Recorremos para ver si están todos los barcos colocados
				for(int i = 0 ; i < 4 ; i++)
				{
					if ( numBarcos[i] == 0)
					{
						total++;
					}
				}
				if( total == 4 )
				{		
			        //nfc_adapter.setNdefPushMessage(ndefMessage, ColocarBarcoActivity.this); //enviamos

					nfc_adapter.setNdefPushMessageCallback(ColocarBarcoActivity.this, ColocarBarcoActivity.this);
					nfc_adapter.setOnNdefPushCompleteCallback(ColocarBarcoActivity.this, ColocarBarcoActivity.this);
					
				}
				else
				{
					Toast.makeText(context, "Aún le quedan" +
							" barcos por colocar", Toast.LENGTH_LONG).show();
				} 
				break;
			case R.id.tblBoard:
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
								cellView.setOnClickListener(new ImageViewClickListener(row, col));
							}
						}
					}
				}
				break;
			}
		}
	};
	private AdapterView.OnItemSelectedListener spilistener = new AdapterView.OnItemSelectedListener() 
	{
		@Override
		public void onItemSelected(AdapterView<?> parent, View v, int position, long id) 
		{
			tvNumBarcos.setText("Quedan " + numBarcos[spiBarcos.getSelectedItemPosition()] + " de ese número de casillas.");
		}
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {}
	};
	
	private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MENSAJE_ENVIADO:
                enviado = true;
            	if( recibido == true )
		        {
		        	pasarActividad();
		        }	
                break;
            }
        }
    };
	
	
	private void limpiar() {
		for(int i=0; i<10; i++)
		{
			for(int j=0; j<10; j++)
			{
				mapaBarcos[i][j] = 0; //Todo agua es decir, todo vacío
			}
		}
		for(int i=4; i>0; i--)
		{
			numBarcos[4-i] = i;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_colocar_barco);
		recibido = false;
		enviado = false;
		go = (Button) findViewById(R.id.bt_go);
		spiBarcos = (Spinner) findViewById(R.id.spinnerBarcos);
		spiOrientacion = (Spinner) findViewById(R.id.spinnerOrientacion);
		tvNumBarcos = (TextView) findViewById(R.id.tvNumBar);
		table = (TableLayout) findViewById(R.id.tblBoard);
		res_barco = getResources().getIdentifier("tilebarco", "drawable", getPackageName());
		nfc_adapter = NfcAdapter.getDefaultAdapter(context);
		
        
		limpiar();
		
		tvNumBarcos.setText("Quedan " + numBarcos[spiBarcos.getSelectedItemPosition()] + " de ese número de casillas.");
		tvNumBarcos.setTextColor(Color.RED);
		//El estilo no he sido capaz de ponerlo aquí así que lo realizo desde el .xml El estilo se puede poner por aquí pero solo es válido para el API 17
		//Y el google nexus utiliza un API inferior
		
		if (nfc_adapter == null) {
            Toast.makeText(context, "NFC no está permitido, la aplicación se cerrará", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
		
		pendingIntent = PendingIntent.getActivity(
                this, 0, 
                new Intent( context, getClass() ).addFlags( Intent.FLAG_ACTIVITY_SINGLE_TOP ), 0);
		
		spiBarcos.setOnItemSelectedListener(spilistener);
		go.setOnClickListener(btlistener);
		table.setOnClickListener(btlistener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.colocar_barco, menu);
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
	protected void onNewIntent( Intent intent )
    {
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) 
    	{
			if ( procesingNFC == false )
			{//Hacemos un hilo para que no sea bloqueante la lectura
				procesingNFC = true;
			    new ReadNfcThread( intent ).start();
			}
    	}
    }
	
	public void pasarActividad()
	{
		Intent intent = new Intent(context , JuegoActivity.class);
		
		if( numeroAleatorio >= valorRecibido) intent.putExtra("turno", 1);
		else								intent.putExtra("turno", 0);
		
		for(int i=0; i<mapaBarcos.length; ++i){ intent.putExtra("mapaBarcos"+i, mapaBarcos[i]);}
		startActivityForResult(intent, REQUEST_CODE_COLOCAR);
		finish();
	}
	
	//Hilo que siempre va a estar escuchando
 	public class ReadNfcThread extends Thread
    {
        private Intent intent;
        
        public ReadNfcThread( Intent intent )
        {
            this.intent = intent;
        }

        public void run()
        {
            Log.d( "NfcRead", "Action: " + intent.getAction() ); 
            try
            {   
            	
                //Recibir o leer mensaje ndef
            	Parcelable[] rMsg = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                NdefMessage msg = (NdefMessage) rMsg[0];
                //Cojo el primero de los rmsg ya que solo es 1 mensaje
                
                NdefRecord[] ndrs = msg.getRecords();
                //Se supone que el primer valor es el OK (no necesitamos saber que es un OK)
                final NdefRecord ndrvalue = ndrs[1];
                    
                
                String textEncoding = "UTF-8"; //Envíamos como UTF-8 así que recibiremos como tal
                int languageCodeLength = "es".getBytes(Charset.forName("UTF-8")).length; 
                
                valorRecibido = Integer.parseInt(new String(ndrvalue.getPayload() , 
                								languageCodeLength + 1 , 
                								ndrvalue.getPayload().length - languageCodeLength - 1 ,
                								textEncoding));
                procesingNFC= false;
                recibido = true;
                if ( enviado == true )
                {
                    runOnUiThread(
                            new Runnable()
                            {
                                public void run() {
                                	pasarActividad();
                                }
                            }
                    );
                }
            }
            catch( Exception e )
            {
                runOnUiThread(
                        new Runnable()
                        {
                            public void run() {
                                Toast.makeText( ColocarBarcoActivity.this, R.string.error_gener, Toast.LENGTH_LONG ).show();
                            }
                        }
                );
            }
        }

    }
 	
 	//Clase creada como listener de un ImageView
	class ImageViewClickListener implements View.OnClickListener
	{
		private int row = 0;
		private int col = 0;
		private boolean colocar = true;

		public ImageViewClickListener(int row, int column)
		{
			//Le coloco un menos 1 poruqe la tabla va de la posición 1 a la 10 y mi array de la 0 a la 9
			//A la hora de colorear le añado una posición para que colore bien la tabla
			this.row = row - 1;
			this.col = column - 1;
		}
		
		
		public void onClick(View v)
		{		
			if(v instanceof ImageView)
			{	
				//Primero comprobamos si el barco cabe en esa posición
				if( mapaBarcos[row][col] == 0 )
				{
					if (numBarcos[spiBarcos.getSelectedItemPosition()] == 0)
					{
						//No hay más barcos
						Toast.makeText(context, "Ya no puede" +
								" colocar más barcos de ese tamaño", Toast.LENGTH_LONG).show();
						colocar = false;
					}
					else if ((col + spiBarcos.getSelectedItemPosition()) > 9 && spiOrientacion.getSelectedItemPosition() == 0 )
					{
						//No entra en la fila
						Toast.makeText(context, "No puede" +
								" colocar el barco ahí, no hay hueco en esa fila", Toast.LENGTH_LONG).show();
						colocar = false;
					}
					else if((row + spiBarcos.getSelectedItemPosition()) > 9 && spiOrientacion.getSelectedItemPosition() == 1)
					{
						//No entra en la columna
						Toast.makeText(context, "No puede" +
								" colocar el barco ahí, no hay hueco en esa columna", Toast.LENGTH_LONG).show();
						colocar = false;
					}
					else
					{
						for(int i = 0 ; i < spiBarcos.getSelectedItemPosition() + 1 && colocar == true ; i++)
						{
							if ( ( spiOrientacion.getSelectedItemPosition() == 0 )
										&& 
										( ( ( row == 9 ) && ( (col + i == 9 && mapaBarcos[row - 1][col - 1 + i] == 1)
														|| (col + i == 0 && mapaBarcos[row - 1][col + 1 + i] == 1) 
														|| (col + i != 0 && col + i != 9 && (mapaBarcos[row - 1][col - 1 + i] == 1 
																						|| mapaBarcos[row - 1][col + 1 + i] == 1) ) 
														|| (mapaBarcos[row - 1][col + i] == 1) ) )
																						
										|| ( ( row == 0 ) && ( ( col + i== 9 && mapaBarcos[row + 1][col - 1 + i] == 1)
															|| (col + i == 0 && mapaBarcos[row + 1][col + 1 + i] == 1)
															|| (col + i!= 0 && col + i!= 9 && (mapaBarcos[row + 1][col - 1 + i] == 1 
																					|| mapaBarcos[row + 1][col + 1 + i] == 1) ) 
															|| (mapaBarcos[row + 1][col + i] == 1) ) )
																					
										|| ( (row != 9 && row != 0) && ( (( col + i == 0 &&  (mapaBarcos[row - 1][col + 1 + i] == 1 
																						|| mapaBarcos[row + 1][col + 1 + i] == 1 ))
																			|| (col + i == 0 && mapaBarcos[row + 1][col + i] == 1)
                                                                            || (col + i == 0 && mapaBarcos[row - 1][col + i] == 1)
                                                                            || (col + i == 0 && mapaBarcos[row][col + i + 1] == 1))
																		|| ( (col + i == 9 &&  (mapaBarcos[row - 1][col + i - 1] == 1 
																							|| mapaBarcos[row + 1][col + i - 1] == 1) ) 
																					|| (col + i == 9 && mapaBarcos[row + 1][col + i] == 1)
                                                                                    || (col + i == 9 && mapaBarcos[row - 1][col + i] == 1)
                                                                                    || (col + i == 9 && mapaBarcos[row][col + i -1] == 1) ) ) )
                                                                                    
										|| ((row == 0 || row == 9) && ( col + i != 0 && col + i != 9)
                                                            && (( mapaBarcos[row][col - 1 + i] == 1 )
                                                                || ( mapaBarcos[row][col + 1 + i] == 1 )))	
                                                                
										|| ( ( row != 0 && row != 9 && col + i != 0 && col + i != 9 ) 
												&& (mapaBarcos[row + 1][col + 1 + i] == 1 
													|| mapaBarcos[row - 1][col - 1 + i] == 1
														|| mapaBarcos[row + 1][col - 1 + i] == 1
															|| mapaBarcos[row - 1][col + 1 + i] == 1
																|| mapaBarcos[row][col + 1 + i] == 1
																	|| mapaBarcos[row][col - 1 + i] == 1
																		|| mapaBarcos[row + 1][col + i] == 1
																			|| mapaBarcos[row - 1][col + i] == 1
																				|| (mapaBarcos[row][col + 1 + i] == 1) 
																					|| (mapaBarcos[row + 1][col + i] == 1)) )
																			
										|| ( mapaBarcos[row][col + i] == 1) ))
							{
								//horizontal
								Toast.makeText(context, "No puede" +
										" colocar el barco ahí, choca con otro barco", Toast.LENGTH_LONG).show();
								colocar = false;
							}
							else if ( ( spiOrientacion.getSelectedItemPosition() == 1 )
									&& 
									( ( ( row + i == 9 ) && ( (col == 9 && mapaBarcos[row - 1 + i][col - 1] == 1)
															|| (col == 0 && mapaBarcos[row - 1 + i][col + 1] == 1) 
															|| (col != 0 && col != 9 && (mapaBarcos[row - 1 + i][col - 1] == 1 
																					|| mapaBarcos[row - 1 + i][col + 1] == 1 ) ) 
															|| (mapaBarcos[row - 1 + i][col] == 1) ) )
																					
									|| ( ( row + i == 0 ) && ( ( col == 9 && mapaBarcos[row + 1 + i][col - 1] == 1)
															|| (col == 0 && mapaBarcos[row + 1 + i][col + 1] == 1)
															|| (col != 0 && col != 9 && (mapaBarcos[row + 1 + i][col - 1] == 1 
																				|| mapaBarcos[row + 1 + i][col + 1] == 1) ) 
															|| (mapaBarcos[row + 1][col + i] == 1) ) )
																				
									|| ( (row + i != 9 && row + i != 0) && ( ( ( col == 0 &&  (mapaBarcos[row - 1 + i][col + 1] == 1 
																						|| mapaBarcos[row + 1 + i][col + 1] == 1 ))
																			|| (col == 0 && mapaBarcos[row + 1 + i][col] == 1)                                      
                                                                            || (col == 0 && mapaBarcos[row - 1 + i][col] == 1)
                                                                            || (col== 0 && mapaBarcos[row + i][col + 1] == 1))
																	|| (( (col == 9 &&  (mapaBarcos[row - 1 + i][col - 1] == 1 
																					|| mapaBarcos[row + 1 + i][col - 1] == 1)) 
																				|| (col == 9 && mapaBarcos[row + 1 + i][col] == 1)
			                                                                            || (col == 9 && mapaBarcos[row - 1 + i][col] == 1)
			                                                                            || (col == 9 && mapaBarcos[row + i][col - 1] == 1)) ) ))
			                        || ((row + i == 0 || row + i == 9) && ( col != 0 && col != 9)
                                                            && (( mapaBarcos[row + i][col - 1] == 1 )
                                                                || ( mapaBarcos[row + i][col + 1] == 1 )))
									
									|| ( ( row + i != 0 && row + i != 9 && col != 0 && col != 9 ) 
											&& (mapaBarcos[row + 1 + i][col + 1] == 1 
												|| mapaBarcos[row - 1 + i][col - 1] == 1
													|| mapaBarcos[row + 1 + i][col - 1] == 1
														|| mapaBarcos[row - 1 + i][col + 1] == 1
															|| mapaBarcos[row + i][col + 1] == 1
																|| mapaBarcos[row + i][col - 1] == 1
																	|| mapaBarcos[row + 1 + i][col] == 1
																		|| mapaBarcos[row - 1 + i][col] == 1
																			|| (mapaBarcos[row + i][col + 1] == 1) 
																				|| (mapaBarcos[row + i + 1][col] == 1)) )
																		
									|| (mapaBarcos[row + i][col] == 1) ) )
							{
								//vertical
								
								Toast.makeText(context, "No puede" +
										" colocar el barco ahí, choca con otro barco", Toast.LENGTH_LONG).show();
								colocar = false;
							}
						}
					}
					
					if ( colocar == true )
					{
						numBarcos[spiBarcos.getSelectedItemPosition()] -= 1;
						tvNumBarcos.setText(String.format("Quedan %d barcos", numBarcos[spiBarcos.getSelectedItemPosition()]));
						TableRow tableRow;
						View cellView;

						if( spiOrientacion.getSelectedItemPosition() == 0)
						{
							for(int i=0; i < (spiBarcos.getSelectedItemPosition() + 1); i++)
							{
								tableRow = (TableRow) table.getChildAt(row + 1);
								cellView = tableRow.getChildAt(col + 1 + i);
								((ImageView) cellView).setImageResource(res_barco);
								mapaBarcos[row][col + i] = 1;
							}

						}

						else if( spiOrientacion.getSelectedItemPosition() == 1)
						{
							for(int i=0; i < (spiBarcos.getSelectedItemPosition() + 1); i++)
							{
								tableRow = (TableRow) table.getChildAt(row + 1 + i);
								cellView = tableRow.getChildAt(col + 1);
								((ImageView) cellView).setImageResource(res_barco);
								mapaBarcos[row + i][col] = 1;
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onNdefPushComplete(NfcEvent event) 
	{
		handler.obtainMessage(MENSAJE_ENVIADO).sendToTarget();
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) 
	{
		Random rd = new Random();
		//Significa que ya se han colocado todos los barcos
		//Envíamos un OK y si ya hemos recibido el OK del otro móvil podemos pasar a la siguiente actividad.
		
		//Así creamos un NdefRecord (no estoy seguro de que funcione correctamente)
		String text_ok = "OK";
		String lang = "es";
		Charset utfEncoding =Charset.forName("UTF-8");
        byte[] textBytes_ok = text_ok.getBytes(utfEncoding);
        byte[] langBytes = lang.getBytes(utfEncoding);
        int langLength = langBytes.length;
        int textLength_ok = textBytes_ok.length;
        byte[] payLoadOk = new byte[1 + langLength + textLength_ok];
        
        
        payLoadOk[0] = (byte) langLength;
 
        System.arraycopy(langBytes, 0, payLoadOk, 1, langLength);
        System.arraycopy(textBytes_ok, 0, payLoadOk, 1 + langLength, textLength_ok);
        
        NdefRecord recordOK = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, 
        									NdefRecord.RTD_TEXT, 
        									new byte[0], payLoadOk);
        
        //Todo lo relacionado con el idioma podemos reutilizarlo
        numeroAleatorio = rd.nextInt();
		String aleatorio = Integer.toString(numeroAleatorio);
		byte[] textBytes_valor = aleatorio.getBytes(utfEncoding);
		int textLength_valor = textBytes_valor.length;
		byte[] payLoadValor = new byte[1 + langLength + textLength_valor];
		
		payLoadValor[0] = (byte) langLength;
		 
	    System.arraycopy(langBytes, 0, payLoadValor, 1, langLength);
	    System.arraycopy(textBytes_valor, 0, payLoadValor, 1 + langLength, textLength_valor);
        
	    NdefRecord recordValor = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, 
	    									NdefRecord.RTD_TEXT, 
	    									new byte[0], payLoadValor);
        
	    //Ya los hemos creado (los dos)
        
        NdefMessage ndefMessage = new NdefMessage(recordOK, recordValor);

		return ndefMessage;
	}

}
