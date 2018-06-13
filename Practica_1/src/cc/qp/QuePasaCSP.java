/**
 * @author Bachiller Javier Barragan Haro y su increible escudero Raul Carbajosa Gonzalez
 * */
package cc.qp;

import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.ChannelOutput;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;

import es.upm.babel.cclib.Monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class QuePasaCSP implements QuePasa, CSProcess, Practica {

	// Creamos un canal por cada operación sin CPRE
	private Any2OneChannel chCrearGrupo = Channel.any2one();
	private Any2OneChannel chAnadirMiembro = Channel.any2one();
	private Any2OneChannel chSalirGrupo = Channel.any2one();
	private Any2OneChannel chMandarMensaje = Channel.any2one();
	// Creamos un canal para solicitar leer
	// Usaremos peticiones aplazadas en el servidor para tratar
	// la CPRE de leer

	public Alumno[] getAutores() {
		return new Alumno[] { new Alumno("Javier Barragan Haro", "y160253"),
				new Alumno("Raul Carbajosa Gonzalez", "y160311")

		};
	}

	private Any2OneChannel chPetLeer = Channel.any2one();

	public QuePasaCSP() {
	}

	// clases auxiliares para realizar peticiones al servidor
	// os regalamos la implementación de CrearGrupo
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
		public One2OneChannel chAnadir;
		public int creadorUid;
		public String grupo;
		public int nuevoMiembroUid;

		public PetAnadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) {
			this.creadorUid = creadorUid;
			this.grupo = grupo;
			this.nuevoMiembroUid = nuevoMiembroUid;
			chAnadir = Channel.one2one();
		}
	}

	public class PetSalirGrupo {
		// TO DO : atributos de la clase
		public One2OneChannel chSalir;
		public int miembroUid;
		public String grupo;

		public PetSalirGrupo(int miembroUid, String grupo) {
			this.miembroUid = miembroUid;
			this.grupo = grupo;
			chSalir = Channel.one2one();
		}
	}

	public class PetMandarMensaje {
		// TO DO: atributos de la clase
		public int remitenteUid;
		public String grupo;
		public Object contenidos;
		public One2OneChannel chMandar;

		public PetMandarMensaje(int remitenteUid, String grupo, Object contenidos) {
			this.remitenteUid = remitenteUid;
			this.grupo = grupo;
			this.contenidos = contenidos;
			chMandar = Channel.one2one();
		}
	}

	public class PetLeer {
		// TO DO: atributos de la clase
		public int uid;
		public One2OneChannel chLeer;

		public PetLeer(int uid) {
			this.uid = uid;
			chLeer = Channel.one2one();
		}
	}

	// Implementamos aquí los métodos de la interfaz QuePasa
	// os regalamos la implementación de crearGrupo
	public void crearGrupo(int creadorUid, String grupo) throws PreconditionFailedException {
		// creamos la petición
		PetCrearGrupo pet = new PetCrearGrupo(creadorUid, grupo);
		// la enviamos
		chCrearGrupo.out().write(pet);
		// recibimos mensaje de status
		Boolean exito = (Boolean) pet.chResp.in().read();
		// si el estado de la petición es negativo, lanzamos excepción
		if (!exito)
			throw new PreconditionFailedException();
	}

	public void anadirMiembro(int uid, String grupo, int nuevoMiembroUid) throws PreconditionFailedException {
		PetAnadirMiembro pet = new PetAnadirMiembro(uid, grupo, nuevoMiembroUid);
		chAnadirMiembro.out().write(pet);
		Boolean exito = (Boolean) pet.chAnadir.in().read();
		if (!exito)
			throw new PreconditionFailedException();
	}

	public void salirGrupo(int uid, String grupo) throws PreconditionFailedException {
		PetSalirGrupo pet = new PetSalirGrupo(uid, grupo);
		chSalirGrupo.out().write(pet);
		Boolean exito = (Boolean) pet.chSalir.in().read();
		if (!exito)
			throw new PreconditionFailedException();
	}

	public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {
		PetMandarMensaje pet = new PetMandarMensaje(remitenteUid, grupo, contenidos);
		chMandarMensaje.out().write(pet);
		Boolean exito = (Boolean) pet.chMandar.in().read();
		if (!exito)
			throw new PreconditionFailedException();
	}

	public Mensaje leer(int uid) {
		PetLeer pet = new PetLeer(uid);
		chPetLeer.out().write(pet);
		return (Mensaje) pet.chLeer.in().read();
	}

	// El servidor va en el metodo run()
	public void run() {

		// Mete aquí tu implementación del estado del recurso
		// (tráela de la práctica 1)

		// Contiene los usuarios con peticiones aplazadas
		ArrayList<Integer> usuarios = new ArrayList<Integer>();

		Map<String, ArrayList<Integer>> miembros = new HashMap<String, ArrayList<Integer>>();
		// Atributo creador: Mapa que tiene como clave el nombre del
		// grupo(String) y
		// como valor el id del creador del grupo(int)
		Map<String, Integer> creador = new HashMap<String, Integer>();
		// Atributo mensaje: Mapa que tiene como clave el id del usuario que lee
		// el
		// mensaje(int)
		// y como valor una LIFO de mensajes(LinkedList<Mensaje>)
		Map<Integer, LinkedList<Mensaje>> mensaje = new HashMap<Integer, LinkedList<Mensaje>>();

		LinkedList<PetLeer> peticiones = new LinkedList<PetLeer>();
		// TO DO
		// TO DO
		// Colección para aplazar peticiones de leer
		// (adapta la que usaste en monitores, pero
		// sustituye las Cond por One2OneChannel)
		// TO DO
		// TO DO

		// Códigos de peticiones para facilitar la asociación
		// de canales a operaciones
		final int CREAR_GRUPO = 0;
		final int ANADIR_MIEMBRO = 1;
		final int SALIR_GRUPO = 2;
		final int MANDAR_MENSAJE = 3;
		final int LEER = 4;

		// recepción alternativa
		final Guard[] guards = new AltingChannelInput[5];
		guards[CREAR_GRUPO] = chCrearGrupo.in();
		guards[ANADIR_MIEMBRO] = chAnadirMiembro.in();
		guards[SALIR_GRUPO] = chSalirGrupo.in();
		guards[MANDAR_MENSAJE] = chMandarMensaje.in();
		guards[LEER] = chPetLeer.in();

		final Alternative services = new Alternative(guards);
		int chosenService;

		while (true) {
			// toda recepcion es incondicional
			chosenService = services.fairSelect();
			switch (chosenService) {
			// regalamos la implementación del servicio de crearGrupo
			case CREAR_GRUPO: {
				// recepción del mensaje
				PetCrearGrupo pet = (PetCrearGrupo) chCrearGrupo.in().read();
				// comprobación de la PRE
				if (creador.containsKey(pet.grupo))
					// status KO
					pet.chResp.out().write(false);
				// ejecución normal
				else {
					// operación
					// TO DO: copia aquí tu implementación
					// de crearGrupo de la práctica 1
					creador.put(pet.grupo, pet.creadorUid);

					ArrayList<Integer> miembros_lista = new ArrayList<Integer>();
					miembros_lista.add(pet.creadorUid);
					miembros.put(pet.grupo, miembros_lista);

					if (mensaje.get(pet.creadorUid) == null) {
						LinkedList<Mensaje> nuevo = new LinkedList<Mensaje>();
						mensaje.put(pet.creadorUid, nuevo);
					}
					// status OK
					pet.chResp.out().write(true);
				}
				break;
			}

			case ANADIR_MIEMBRO: {
				// recepcion del mensaje
				PetAnadirMiembro pet = (PetAnadirMiembro) chAnadirMiembro.in().read();
				// comprobacion de la PRE
				if (!creador.containsValue(pet.creadorUid) || miembros.get(pet.grupo).contains(pet.nuevoMiembroUid)) {
					// status KO
					pet.chAnadir.out().write(false);
				} else {
					// ejecución normal

					ArrayList<Integer> listaActualizada = miembros.get(pet.grupo);
					listaActualizada.add(pet.nuevoMiembroUid);
					miembros.remove(pet.grupo);
					miembros.put(pet.grupo, listaActualizada);
					if (mensaje.get(pet.nuevoMiembroUid) == null) {
						LinkedList<Mensaje> nuevo = new LinkedList<Mensaje>();
						mensaje.put(pet.nuevoMiembroUid, nuevo);
					}
					// status OK
					pet.chAnadir.out().write(true);
				}
				break;
			}

			case SALIR_GRUPO: {
				// recepcion de la peticion
				PetSalirGrupo pet = (PetSalirGrupo) chSalirGrupo.in().read();
				// comprobacion de la PRE
				if ((creador.get(pet.grupo) == null || miembros.get(pet.grupo) == null)
						|| (!miembros.get(pet.grupo).contains(pet.miembroUid)
								&& !creador.get(pet.grupo).equals(pet.miembroUid))) {
					// status KO
					pet.chSalir.out().write(false);

					// ejecución normal
				} else {

					LinkedList<Mensaje> borrados = mensaje.get(pet.miembroUid);
					for (int i = 0; i < borrados.size(); i++) {
						if (borrados.get(i).getGrupo().equals(pet.grupo)) {
							borrados.remove(i);
							i--;
						}
					}

					mensaje.remove(pet.miembroUid);
					mensaje.put(pet.miembroUid, borrados);

					ArrayList<Integer> listaActualizada = miembros.get(pet.grupo);
					listaActualizada.remove((Object) pet.miembroUid);
					miembros.remove(pet.grupo);
					miembros.put(pet.grupo, listaActualizada);
					// status OK
					pet.chSalir.out().write(true);
				}
				break;
			}
			case MANDAR_MENSAJE: {
				// recepcion de la peticion
				PetMandarMensaje pet = (PetMandarMensaje) chMandarMensaje.in().read();
				// Comprobacion de la Precondicion
				if (miembros.get(pet.grupo) == null || !miembros.get(pet.grupo).contains(pet.remitenteUid)) {
					pet.chMandar.out().write(false);
				} else {
					ArrayList<Integer> n_miembros = miembros.get(pet.grupo);
					Mensaje msge = new Mensaje(pet.remitenteUid, pet.grupo, pet.contenidos);
					// Se anade el mensaje a la cola de mensajes asociada a cada
					// uid
					for (int i = 0; i < n_miembros.size(); i++) {
						LinkedList<Mensaje> aux = mensaje.get(n_miembros.get(i));
						aux.addLast(msge);
						mensaje.put(n_miembros.get(i), aux);
					}
					pet.chMandar.out().write(true);
				}
				break;
			}
			case LEER: {
				// recepcion de la peticion
				// TO DO
				// no hay PRE que comprobar!
				// TO DO: aquí lo más sencillo
				// TO DO es guardar la petición
				// TO DO según se recibe
				// TO DO (reutilizad la estructura que
				// TO DO usasteis en monitores
				// TO DO cambiando Cond por One2OneChannel)
				PetLeer pet = (PetLeer) chPetLeer.in().read();

				// Si no existe la entrada en el map para el uid se crea
				usuarios.add(pet.uid);
				peticiones.addLast(pet);
				break;
			}
			} // END SWITCH

			// código de desbloqueos
			// solo hay peticiones aplazadas de leer
			// TO DO: recorred la estructura
			// con las peticiones aplazadas
			// y responded a todas aquellas
			// cuya CPRE se cumpl

			for (int i = 0; i < usuarios.size(); i++) {

				// Coprobaciones de desboqueo
				if (usuarios != null && usuarios.get(i) != null && mensaje != null
						&& mensaje.get(usuarios.get(i)) != null && peticiones != null && !peticiones.isEmpty()
						&& !mensaje.get(usuarios.get(i)).isEmpty() && mensaje.get(usuarios.get(i)) != null) {

					// Se comprueba si la peticion que se quiere desbloquear es
					// igaul a uno de los usuarios bloqueados, si es igual se
					// elimina el usuario y se manda el mensaje correspondiente

					if (peticiones.getFirst().uid == usuarios.get(i)) {

						// Se elimina el mensaje asociado al uid de la peticion

						LinkedList<Mensaje> aux = mensaje.get(usuarios.get(i));
						// Se manda el mensaje por el canal de la peticion

						peticiones.getFirst().chLeer.out().write(aux.pop());
						mensaje.remove(usuarios.get(i));
						mensaje.put(usuarios.get(i), aux);

						// Se eliminan la peticion y el usuario asociado a dicha
						// peticion
						peticiones.removeFirst();
						usuarios.remove(i);
						i--;

					}

					// Si la peticion que se intenta desbloquear no es igual al
					// usuario, se mete la peticion al final de la cola y se
					// vuelve a comprobar par a el mismo usuario
					else {
						peticiones.addLast(peticiones.getFirst());
						peticiones.removeFirst();
						i--;
					}
				}
			}

		} // END while(true) SERVIDOR
	} // END run()

}// END class QuePasaCSP
