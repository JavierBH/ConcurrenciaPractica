package cc.qp;

import java.lang.management.MonitorInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import es.upm.babel.cclib.*;

public class QuePasaMonitor implements QuePasa, Practica {
	// ATRIBUTOS:
	// Atributo miembros:Mapa que tiene como clave el nombre del grupo(String) y
	// como valor una lista con los id de los miembros del grupo
	// (ArrayList<Integer>)
	private ArrayList<Integer> usuarios = new ArrayList<Integer>();
	private Map<String, ArrayList<Integer>> miembros = new HashMap<String, ArrayList<Integer>>();
	// Atributo creador: Mapa que tiene como clave el nombre del grupo(String) y
	// como valor el id del creador del grupo(int)
	private Map<String, Integer> creador = new HashMap<String, Integer>();
	// Atributo mensaje: Mapa que tiene como clave el id del usuario que lee el
	// mensaje(int)
	// y como valor una LIFO de mensajes(LinkedList<Mensaje>)
	private Map<Integer, LinkedList<Mensaje>> mensaje = new HashMap<Integer, LinkedList<Mensaje>>();
	// Atributo conditions: Mapa que tiene como clave el id del usuario que lee
	// el mensaje(int)
	// y como valor una condicion(LinkedList<Monitor.Cond>)
	private Map<Integer, Monitor.Cond> conditions = new HashMap<Integer, Monitor.Cond>();
	// Monitor de exclusión mutua
	private Monitor mutex;
	private Monitor.Cond leer;

	public QuePasaMonitor() {
		mutex = new Monitor();
		leer = mutex.newCond();
	}

	/**
	 * @param String
	 *            creadorUid
	 * @param String
	 *            grupo Crea un grupo de QuePasa con el nombre de "grupo" cuyo
	 *            creador tiene el id "creadorUid"
	 * @return void
	 * @throws PreconditionFailedException
	 */

	@Override
	public void crearGrupo(int creadorUid, String grupo) throws PreconditionFailedException {
		mutex.enter();
		// Comprobacion de la PreCondicion

		if (creador.containsKey(grupo)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}

		creador.put(grupo, creadorUid);
		ArrayList<Integer> miembros_lista = new ArrayList<Integer>();
		miembros_lista.add(creadorUid);
		miembros.put(grupo, miembros_lista);
		usuarios.add(creadorUid);
		if (mensaje.get(creadorUid) == null) {
			LinkedList<Mensaje> nuevo = new LinkedList<Mensaje>();
			mensaje.put(creadorUid, nuevo);
		}
		mutex.leave();

	}

	/**
	 * @param String
	 *            creadorUid
	 * @param String
	 *            grupo
	 * @param int
	 *            nuevoMiembroUid El usuario "creadorUid" añade un nuevo
	 *            miembro cuyo uid es "nuevoMiembroUid" al grupo
	 * @return void
	 * @throws PreconditionFailedException
	 */

	@Override
	public void anadirMiembro(int creadorUid, String grupo, int nuevoMiembroUid) throws PreconditionFailedException {
		mutex.enter();
		// Si el creadorUid no es el creador del grupo o el nuevoMiembroUid ya
		// esta en el grupo salta una excepcion
		if (!creador.containsValue(creadorUid) || miembros.get(grupo).contains(nuevoMiembroUid)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}
		this.usuarios.add(nuevoMiembroUid);
		ArrayList<Integer> listaActualizada = miembros.get(grupo);
		listaActualizada.add(nuevoMiembroUid);
		miembros.remove(grupo);
		miembros.put(grupo, listaActualizada);
		LinkedList<Mensaje> nuevo = new LinkedList<Mensaje>();
		mensaje.put(nuevoMiembroUid, nuevo);
		mutex.leave();
	}

	/**
	 * @param String
	 *            miembroUid
	 * @param String
	 *            grupo El usuario "miembroUid" sale del grupo
	 * @return void
	 * @throws PreconditionFailedException
	 */
	@Override
	public void salirGrupo(int miembroUid, String grupo) throws PreconditionFailedException {
		mutex.enter();

		// Comprobacion de la Precondicion
		if ((creador.get(grupo) == null || miembros.get(grupo) == null)
				|| (!miembros.get(grupo).contains(miembroUid) && !creador.get(grupo).equals(miembroUid))) {
			mutex.leave();
			throw new PreconditionFailedException();
		}

		LinkedList<Mensaje> borrados = mensaje.get(miembroUid);
		// Se eliminan todos los mensajes asociados a @grupo
		for (int i = 0; i < borrados.size(); i++) {
			if (borrados.get(i).getGrupo().equals(grupo)) {
				borrados.remove(i);
			}
		}
		for (int j = 0; j < this.usuarios.size(); j++) {
			if (j == miembroUid)
				this.usuarios.remove(miembroUid);
		}
		mensaje.remove(miembroUid);
		mensaje.put(miembroUid, borrados);

		// Se actualiza la lista de mensajes del miembro
		ArrayList<Integer> listaActualizada = miembros.get(grupo);
		listaActualizada.remove((Object) miembroUid);
		miembros.remove(grupo);
		miembros.put(grupo, listaActualizada);
		mutex.leave();
	}

	/**
	 * @param int
	 *            remitenteUid
	 * @param String
	 *            grupo
	 * @param Object
	 *            contenidos El usuario "remitenteUid" manda un mensaje
	 *            "contenidos" por el grupo
	 * @return void
	 * @throws PreconditionFailedException
	 */
	@Override
	public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {
		mutex.enter();

		// Comprobacion de la Precondicion
		if (miembros.get(grupo) == null || !miembros.get(grupo).contains(remitenteUid)) {
			mutex.leave();
			throw new PreconditionFailedException();
		}

		ArrayList<Integer> n_miembros = miembros.get(grupo);
		Mensaje msge = new Mensaje(remitenteUid, grupo, contenidos);
		// Se anade el mensaje a la cola de mensajes asociada a cada uid
		// desbloquear();
		for (int i = 0; i < n_miembros.size(); i++) {
			LinkedList<Mensaje> aux = mensaje.get(n_miembros.get(i));
			aux.addLast(msge);
			mensaje.put(n_miembros.get(i), aux);
		}
		desbloquear();
		mutex.leave();
	}

	/**
	 * @param int
	 *            uid Lee el primer mensaje disponible de lista de mensaje(uid)
	 * @return Mensaje
	 * @throws PreconditionFailedException
	 */
	@Override
	public Mensaje leer(int uid) {
		mutex.enter();
		if (mensaje.get(uid) == null || mensaje.get(uid).isEmpty()) {

			// Si no existe la entrada en el map para el uid se crea

			if (this.conditions.get(uid) == null || this.conditions.isEmpty()) {
				Monitor.Cond aux = mutex.newCond();
				this.conditions.put(uid, aux);
			}
			// Se pone en await la condition
			this.conditions.get(uid).await();

			// Se desbloquean todas las conditions asociadas a esa entrada del
			// map

		}
		LinkedList<Mensaje> aux = mensaje.get(uid);
		Mensaje msge = aux.pop();

		mensaje.remove(uid);
		mensaje.put(uid, aux);
		leer.signal();
		mutex.leave();
		return msge;
	}

	/**
	 * @param int
	 *            uid Desbloquea una condition de la lista conditions(uid) luego
	 *            elimina la condition
	 * @return void
	 */

	public void desbloquear() {
		for (int i = 0; i < usuarios.size(); i++) {
			if (this.usuarios != null && this.usuarios.get(i) != null && this.conditions.get(usuarios.get(i)) != null
					&& this.conditions.get(usuarios.get(i)).waiting() > 0
					&& !this.mensaje.get(usuarios.get(i)).isEmpty()) {
				this.conditions.get(usuarios.get(i)).signal();
				leer.await();
			}
		}
	}

	@Override
	public Alumno[] getAutores() {
		return new Alumno[] { new Alumno("Javier Barragan Haro", "y160253"),
				new Alumno("Raul Carbajosa Gonzalez", "y160311")

		};

	}
}
