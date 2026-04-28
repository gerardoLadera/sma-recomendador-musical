package com.sma.agentes;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.ArrayList;
import java.util.List;

public class AgenteHistorial extends Agent {


    private List<String> memoriaHistorial;

    @Override
    protected void setup() {
        memoriaHistorial = new ArrayList<>();
        System.out.println("Agente  [" + getLocalName() + "] iniciado. Auditoría activa...");

        // 1. Registro en el DF local
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("historial-musical");
        sd.setName("servicio-historial");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // 2. Escucha y registro
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null && msg.getPerformative() == ACLMessage.INFORM) {

                    String contenido = msg.getContent(); // Se espera formato "Cancion|Puntaje"

                    try {
                        // Procesar la cadena
                        String[] partes = contenido.split("\\|");
                        String nombreCancion = partes[0];
                        String score = partes[1];

                        // Guardar en la lista interna
                        String registroCompleto = "Canción: " + nombreCancion + " | Match: " + score + "%";
                        memoriaHistorial.add(registroCompleto);

                        // Imprimir el formato
                        System.out.println("\n--------------------------------------------------");
                        System.out.println(">>> [REGISTRO]: Canción: " + nombreCancion + " | Precisión: " + score + "%");
                        System.out.println(">>> Total de canciones en la sesión actual: " + memoriaHistorial.size());
                        System.out.println("--------------------------------------------------\n");

                    } catch (Exception e) {
                        System.err.println("Historial: Error al procesar el mensaje recibido -> " + contenido);
                    }

                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        try { DFService.deregister(this); } catch (Exception e) {}
    }
}