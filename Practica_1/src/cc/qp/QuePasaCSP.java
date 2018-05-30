package cc.qp;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
//import es.upm.babel.cclib.*;

public class QuePasaCSP implements QuePasa, CSProcess {

	// Creamos un canal por cada operaciÃ³n sin CPRE
	private Any2OneChannel chCrearGrupo = Channel.any2one();
	private Any2OneChannel chAnadirMiembro = Channel.any2one();
	private Any2OneChannel chSalirGrupo = Channel.any2one();
	private Any2OneChannel chMandarMensaje = Channel.any2one();
	// Creamos un canal para solicitar leer
	// Usaremos peticiones aplazadas en el servidor para tratar
	// la CPRE de leer
	private Any2OneChannel chPetLeer = Channel.any2one();

	public QuePasaCSP() {
	}

	// clases auxiliares para realizar peticiones al servidor
	// os regalamos la implementaciÃ³n de CrearGrupo
	public class PetCrearGrupo {
		public int creadorUid;
		public String grupo;
		// para tratamiento de la PRE
		public One2OneChannel chResp;

		public PetCrearGrupo(int creadorUid, String grupo) {
			this.creadorUid = creadorUid;
			this.grupo = grupo;
			this.chResp = Channel.one2one();
		}
	}

	public class PetAnadirMiembro {
		public One2OneChannel pre;
		public int creadorUid;
		public String grupo;
		public int nuevoMiembroUid;

		public PetAnadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) {
			this.creadorUid = creadorUid;
			this.grupo = grupo;
			this.nuevoMiembroUid = nuevoMiembroUid;
			pre = Channel.one2one();
		}
	}

	public class PetSalirGrupo {
		// TO DO : atributos de la clase
		public One2OneChannel pre;
		public int miembroUid;
		public String grupo;

		public PetSalirGrupo(int miembroUid, String grupo) {
			this.miembroUid = miembroUid;
			this.grupo = grupo;
			pre = Channel.one2one();
		}
	}

	public class PetMandarMensaje {
		// TO DO: atributos de la clase
		public int remitenteUid;
		public String grupo;
		public Object contenidos;
		public One2OneChannel pre;

		public PetMandarMensaje(int remitenteUid, String grupo, Object contenidos) {
			this.remitenteUid = remitenteUid;
			this.grupo = grupo;
			this.contenidos = contenidos;
			pre = Channel.one2one();
		}
	}

	public class PetLeer {
		// TO DO: atributos de la clase
		public int uid;
		public One2OneChannel pre;

		public PetLeer(int uid) {
			this.uid = uid;
			pre = Channel.one2one();
		}
	}

	// Implementamos aquÃ­ los mÃ©todos de la interfaz QuePasa
	// os regalamos la implementaciÃ³n de crearGrupo
	public void crearGrupo(int creadorUid, String grupo) throws PreconditionFailedException {
		// creamos la peticiÃ³n
		PetCrearGrupo pet = new PetCrearGrupo(creadorUid, grupo);
		// la enviamos
		chCrearGrupo.out().write(pet);
		// recibimos mensaje de status
		Boolean exito = (Boolean) pet.chResp.in().read();
		// si el estado de la peticiÃ³n es negativo, lanzamos excepciÃ³n
		if (!exito)
			throw new PreconditionFailedException();
	}

	public void anadirMiembro(int uid, String grupo, int nuevoMiembroUid) throws PreconditionFailedException {
		// creamos la peticiÃ³n
		PetAnadirMiembro pet = new PetAnadirMiembro(uid, grupo,nuevoMiembroUid);
		// la enviamos
		chAnadirMiembro.out().write(pet);
		// recibimos mensaje de status
		Boolean exito = (Boolean) pet.pre.in().read();
		// si el estado de la peticiÃ³n es negativo, lanzamos excepciÃ³n
		if (!exito)
			throw new PreconditionFailedException();
	}

	public void salirGrupo(int uid, String grupo) throws PreconditionFailedException {
		// creamos la peticiÃ³n
		PetSalirGrupo pet = new PetSalirGrupo(uid, grupo);
		// la enviamos
		chSalirGrupo.out().write(pet);
		// recibimos mensaje de status
		Boolean exito = (Boolean) pet.pre.in().read();
		// si el estado de la peticiÃ³n es negativo, lanzamos excepciÃ³n
		if (!exito)
			throw new PreconditionFailedException();
	}

	public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {
		// creamos la peticiÃ³n
		PetMandarMensaje pet = new PetMandarMensaje(remitenteUid, grupo,contenidos);
		// la enviamos
		chMandarMensaje.out().write(pet);
		// recibimos mensaje de status
		Boolean exito = (Boolean) pet.pre.in().read();
		// si el estado de la peticiÃ³n es negativo, lanzamos excepciÃ³n
		if (!exito)
			throw new PreconditionFailedException();
	}

	public Mensaje leer(int uid) {
		// creamos la peticiÃ³n
		PetLeer pet = new PetLeer(uid);
		// la enviamos
		chMandarMensaje.out().write(pet);
		// recibimos mensaje de status
		Mensaje resultado = (Mensaje) pet.pre.in().read();
		return resultado;
	}

	// El servidor va en el mÃ©todo run()
	public void run() {

		//ATRIBUTOS:
		//Atributo miembros:Mapa que tiene como clave el nombre del grupo(String) y como valor una lista con los id de los miembros del grupo (ArrayList<Integer>) 
		Map<String, ArrayList<Integer>> miembros = new HashMap<String, ArrayList<Integer>>();
		//Atributo creador: Mapa que tiene como clave el nombre del grupo(String) y como valor el id del creador del grupo(int)
		Map<String, Integer> creador = new HashMap<String, Integer>();
		//Atributo mensaje: Mapa que tiene como clave el id del usuario que lee el mensaje(int) 
		//y como valor una LIFO de mensajes(LinkedList<Mensaje>) 
		Map<Integer, LinkedList<Mensaje>> mensaje = new HashMap<Integer, LinkedList<Mensaje>>();

		// ColecciÃ³n para aplazar peticiones de leer
		// (adapta la que usaste en monitores, pero
		//  sustituye las Cond por One2OneChannel)
		Map<Integer,LinkedList<PetLeer>> petitions= new HashMap<Integer,LinkedList<PetLeer>>();

		// CÃ³digos de peticiones para facilitar la asociaciÃ³n
		// de canales a operaciones 
		final int CREAR_GRUPO    = 0;
		final int ANADIR_MIEMBRO = 1;
		final int SALIR_GRUPO    = 2;
		final int MANDAR_MENSAJE = 3;
		final int LEER           = 4;

		// recepciÃ³n alternativa
		final Guard[] guards = new AltingChannelInput[5];
		guards[CREAR_GRUPO]    = chCrearGrupo.in();
		guards[ANADIR_MIEMBRO] = chAnadirMiembro.in();
		guards[SALIR_GRUPO]    = chSalirGrupo.in();
		guards[MANDAR_MENSAJE] = chMandarMensaje.in();
		guards[LEER]           = chPetLeer.in();

		final Alternative services = new Alternative(guards);
		int chosenService;

		while (true) {
			// toda recepcion es incondicional
			chosenService = services.fairSelect();
			switch (chosenService) {
			// regalamos la implementaciÃ³n del servicio de crearGrupo
			case CREAR_GRUPO: {
				// recepciÃ³n del mensaje
				PetCrearGrupo pet = (PetCrearGrupo) chCrearGrupo.in().read();
				// comprobaciÃ³n de la PRE
				if (creador.containsKey(pet.grupo))
					pet.chResp.out().write(false);
				// ejecuciÃ³n normal
				else {
					creador.put(pet.grupo, pet.creadorUid);
					ArrayList<Integer> miembros_lista = new ArrayList<Integer>();
					miembros_lista.add(pet.creadorUid);
					miembros.put(pet.grupo, miembros_lista);
					if (mensaje.get(pet.creadorUid) == null) {
						LinkedList<Mensaje> nuevo = new LinkedList<Mensaje>();
						mensaje.put(pet.creadorUid, nuevo);
					}
					pet.chResp.out().write(true);
				}
				break;
			}
			case ANADIR_MIEMBRO: {
				PetAnadirMiembro pet =(PetAnadirMiembro)chAnadirMiembro.in().read();
				// comprobacion de la PRE
				if(!creador.containsValue(pet.creadorUid) || 
						miembros.get(pet.grupo).contains(pet.nuevoMiembroUid))
					pet.pre.out().write(false);
				else {
					ArrayList<Integer> listaActualizada = miembros.get(pet.grupo);
					listaActualizada.add(pet.nuevoMiembroUid);
					miembros.remove(pet.grupo);
					miembros.put(pet.grupo, listaActualizada);
					LinkedList<Mensaje> nuevo = new LinkedList<Mensaje>();
					mensaje.put(pet.nuevoMiembroUid, nuevo);
				}
				break;
			}
			case SALIR_GRUPO: {
				PetSalirGrupo pet=(PetSalirGrupo)chSalirGrupo.in().read();
				if((creador.get(pet.grupo) == null || miembros.get(pet.grupo) == null)
						|| (!miembros.get(pet.grupo).contains(pet.miembroUid) 
								&& !creador.get(pet.grupo).equals(pet.miembroUid)))
					pet.pre.out().write(false);
				else {
					LinkedList<Mensaje> borrados = mensaje.get(pet.miembroUid);
					for (int i = 0; i < borrados.size(); i++) {
						if (borrados.get(i).getGrupo().equals(pet.grupo)) {
							borrados.remove(i);
						}
					}
					mensaje.remove(pet.miembroUid);
					mensaje.put(pet.miembroUid, borrados);
					ArrayList<Integer> listaActualizada = miembros.get(pet.grupo);
					listaActualizada.remove((Object)pet.miembroUid);
					miembros.remove(pet.grupo);
					miembros.put(pet.grupo, listaActualizada);
				}
				break;
			}
			case MANDAR_MENSAJE: {
				PetMandarMensaje pet=(PetMandarMensaje)chMandarMensaje.in().read();
				if(miembros.get(pet.grupo) == null || !miembros.get(pet.grupo).contains(pet.remitenteUid))
					pet.pre.out().write(false);
				else {
					ArrayList<Integer> n_miembros = miembros.get(pet.grupo);
					Mensaje msge = new Mensaje(pet.remitenteUid, pet.grupo, pet.contenidos);
					for (int i = 0; i < n_miembros.size(); i++) {
						LinkedList<Mensaje> aux = mensaje.get(n_miembros.get(i));
						aux.addLast(msge);
						mensaje.put(n_miembros.get(i), aux);
						desbloquear(n_miembros.get(i));
					}
				}
				break;
			}
			case LEER: {
				// recepcion de la peticion
				// TO DO
				// no hay PRE que comprobar!
				// TO DO: aquÃ­ lo mÃ¡s sencillo 
				// TO DO  es guardar la peticiÃ³n
				// TO DO  segÃºn se recibe
				// TO DO  (reutilizad la estructura que 
				// TO DO   usasteis en monitores
				// TO DO   cambiando Cond por One2OneChannel)
				// TO DO
				// TO DO
				// TO DO
				// TO DO
				break;
			}
			} // END SWITCH

			// cÃ³digo de desbloqueos
			// solo hay peticiones aplazadas de leer
			// TO DO: recorred la estructura 
			//        con las peticiones aplazadas
			//        y responded a todas aquellas 
			//        cuya CPRE se cumpla
			public void desbloquear(int uid) {
				if (!(petitions.get(uid) == null) && !petitions.get(uid).isEmpty()) {
					petitions.get(uid).pop();
				}
			}
		} // END while(true) SERVIDOR
	} // END run()
} // END class QuePasaCSP
