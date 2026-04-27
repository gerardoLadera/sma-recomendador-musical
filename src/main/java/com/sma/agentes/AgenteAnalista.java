package com.sma.agentes;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.time.LocalTime;

public class AgenteAnalista extends Agent {

    @Override
    protected void setup() {
        // 1. Registro en el DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("analista-musical");
        sd.setName(getLocalName());
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("[Analista] Registrado en DF como analista-musical");
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println("[Analista] Listo y esperando mensajes...");

        // 2. Comportamiento cíclico de escucha
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                // Para pruebas: acepta cualquier REQUEST, incluido dummy agents
                // Comentar si se esta en produccion
                // MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);

                // Para producción: cambiar a mt con filtro de Oyente
                // --- Descomentar esto para producción (solo escucha al Oyente) ---
                MessageTemplate mt = MessageTemplate.and(
                        MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                        MessageTemplate.MatchSender(new AID("Oyente", AID.ISLOCALNAME)));

                ACLMessage msg = receive(mt);

                if (msg != null) {
                    System.out.println("[Analista] Petición recibida de: " + msg.getSender().getName());

                    String perfil = calcularPerfil(msg.getContent());

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(perfil);
                    send(reply);

                    System.out.println("[Analista] Perfil enviado: " + perfil);
                } else {
                    block();
                }
            }
        });
    }

    private String calcularPerfil(String contenidoMensaje) {
        int hora;

        // Fix: si el contenido es null o vacío, usar hora real directamente
        if (contenidoMensaje == null || contenidoMensaje.trim().isEmpty()) {
            hora = LocalTime.now().getHour();
            System.out.println("[Analista] Sin contenido, usando hora real: " + hora + ":00");
        } else {
            try {
                hora = Integer.parseInt(contenidoMensaje.trim());
                System.out.println("[Analista] Usando hora de prueba: " + hora + ":00");
            } catch (NumberFormatException e) {
                hora = LocalTime.now().getHour();
                System.out.println("[Analista] Usando hora real del sistema: " + hora + ":00");
            }
        }

        String genero;
        int energia;
        int mood;

        if (hora >= 22 || hora < 6) {
            // Noche/madrugada → Lofi, baja energía, mood melancólico
            genero = "Lofi";
            energia = (hora >= 0 && hora < 3) ? 1 : 2;
            mood = 2;
        } else if (hora >= 6 && hora < 12) {
            // Mañana → Rock, energía media, mood neutro
            genero = "Rock";
            energia = 3;
            mood = 3;
        } else if (hora >= 12 && hora < 18) {
            // Tarde → Rock, pico de energía y mood
            genero = "Rock";
            energia = 5;
            mood = 5;
        } else {
            // Noche temprana (18-21) → Rock bajando
            genero = "Rock";
            energia = 3;
            mood = 3;
        }

        System.out.println("[Analista] Genero: " + genero + " | Energia: " + energia + " | Mood: " + mood);
        return genero + ";" + energia + ";" + mood;
    }

    @Override
    protected void takeDown() {
        // Desregistrarse del DF al apagarse
        try {
            DFService.deregister(this);
            System.out.println("[Analista] Desregistrado del DF.");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
