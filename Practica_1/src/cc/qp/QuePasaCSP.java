/**
 * @author Bachiller Javier Barrag醤 Haro y su increible escudero Raul Carbajosa Gonzalez
 * */
package cc.qp;


import org.jcsp.lang.Alternative;
import org.jcsp.lang.AltingChannelInput;
import org.jcsp.lang.Any2OneChannel;
import org.jcsp.lang.CSProcess;
import org.jcsp.lang.Channel;
import org.jcsp.lang.Guard;
import org.jcsp.lang.One2OneChannel;

// TO DO: importad aqu铆 lo que hay谩is usado para implementar
//        el estado del recurso
// TO DO
// TO DO
// TO DO 
// TO DO
// TO DO
// TO DO

public class QuePasaCSP implements QuePasa, CSProcess {

	// Creamos un canal por cada operaci贸n sin CPRE
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
	// os regalamos la implementaci贸n de CrearGrupo
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
		public One2OneChannel pre;

		public PetLeer(int uid) {
			this.uid = uid;
			pre = Channel.one2one();
		}
	}

	// Implementamos aqu铆 los m茅todos de la interfaz QuePasa
	// os regalamos la implementaci贸n de crearGrupo
	public void crearGrupo(int creadorUid, String grupo) throws PreconditionFailedException {
		// creamos la petici贸n
		PetCrearGrupo pet = new PetCrearGrupo(creadorUid, grupo);
		// la enviamos
		chCrearGrupo.out().write(pet);
		// recibimos mensaje de status
		Boolean exito = (Boolean) pet.chResp.in().read();
		// si el estado de la petici贸n es negativo, lanzamos excepci贸n
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
		PetSalirGrupo pet = new PetSalirGrupo(uid,grupo);
		chSalirGrupo.out().write(pet);
		Boolean exito = (Boolean) pet.chSalir.in().read();
		if (!exito)
			throw new PreconditionFailedException();
	}

	public void mandarMensaje(int remitenteUid, String grupo, Object contenidos) throws PreconditionFailedException {
		PetMandarMensaje pet = new PetMandarMensaje(remitenteUid,grupo,contenidos);
		chMandarMensaje.out().write(pet);
		Boolean exito = (Boolean) pet.chMandar.in().read();
		if (!exito)
			throw new PreconditionFailedException();
	}

	public Mensaje leer(int uid) {
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		return null;
	}

	// El servidor va en el metodo run()
	public void run() {

	// Mete aqu铆 tu implementaci贸n del estado del recurso
	// (tr谩ela de la pr谩ctica 1)
	// TO DO
	// TO DO
	// TO DO
	// TO DO
	// TO DO
	// TO DO
	// Colecci贸n para aplazar peticiones de leer
	// (adapta la que usaste en monitores, pero
	//  sustituye las Cond por One2OneChannel)
	// TO DO
	// TO DO

	// C贸digos de peticiones para facilitar la asociaci贸n
	// de canales a operaciones 
	final int CREAR_GRUPO    = 0;
	final int ANADIR_MIEMBRO = 1;
	final int SALIR_GRUPO    = 2;
	final int MANDAR_MENSAJE = 3;
	final int LEER           = 4;

	// recepci贸n alternativa
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
		// regalamos la implementaci贸n del servicio de crearGrupo
	    case CREAR_GRUPO: {
		// recepci贸n del mensaje
		PetCrearGrupo pet = (PetCrearGrupo) chCrearGrupo.in().read();
		// comprobaci贸n de la PRE
		if (/* TO DO: copia aqu铆 tu implementaci贸n de la PRE */)
		    // status KO
		    pet.chResp.out().write(false);
		// ejecuci贸n normal
		else {
		    // operaci贸n
		    // TO DO: copia aqu铆 tu implementaci贸n
		    //        de crearGrupo de la pr谩ctica 1
		    // TO DO
		    // TO DO
		    // TO DO
		    // TO DO
		    // TO DO
		    // status OK
		    pet.chResp.out().write(true);
		}
		break;
	    }
	    case ANADIR_MIEMBRO: {
		// recepcion del mensaje
		// TO DO
		// comprobacion de la PRE
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// ejecucion normal
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		break;
	    }
	    case SALIR_GRUPO: {
		// recepcion de la peticion
		// TO DO
		// comprobacion de la PRE
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// ejecucion normal
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		break;
	    }
	    case MANDAR_MENSAJE: {
		// recepcion de la peticion
		// TO DO
		// comprobacion de la PRE
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// ejecucion normal
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		// TO DO
		break;
	    }
	    case LEER: {
		// recepcion de la peticion
		// TO DO
		// no hay PRE que comprobar!
		// TO DO: aqu铆 lo m谩s sencillo 
		// TO DO  es guardar la petici贸n
		// TO DO  seg煤n se recibe
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

	    // c贸digo de desbloqueos
	    // solo hay peticiones aplazadas de leer
	    // TO DO: recorred la estructura 
	    //        con las peticiones aplazadas
	    //        y responded a todas aquellas 
	    //        cuya CPRE se cumpla
	    // TO DO
	    // TO DO
            // TO DO
	    // TO DO
            // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	    // TO DO
	} // END while(true) SERVIDOR
    } // END run()
} // END class QuePasaCSP
