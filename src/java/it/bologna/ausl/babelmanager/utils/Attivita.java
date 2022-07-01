package it.bologna.ausl.babelmanager.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import java.util.TimeZone;

/**
 *
 * @author utente
 */
public class Attivita{
    private String idAttivita;
    private String idUtente;
    private int tipoAttivita;
    private String descrizioneAttivita;
    private String urlCommand1;
    private String statoAttivita;
    private String dataIn;
    private String noteAttivita;
    private String urlCommand2;
    private String urlCommand3;
    private String labelUrlCommand1;
    private String labelUrlCommand2;
    private String labelUrlCommand3;
    private String uuidAnteprima;
    private String provenienza;
    private String priorita;
    private String oggettoAttivita;
    private String idSetAttivita;
    private String idEsterno;
    private String customApp1;
    private String customApp2;
    private String customApp3;
    private String customApp4;
    private String customApp5;
    private String permesso;
    private String idSorgente;
    private String tipoSorgente;
    private String idRiferimento;
    private String tipoRiferimento;
    private String gruppo;


    public Attivita(){
    }

    public String getIdAttivita() {
        return idAttivita;
    }

    public void setIdAttivita(String idAttivita) {
        this.idAttivita = idAttivita;
    }

    public String getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(String idUtente) {
        this.idUtente = idUtente;
    }

    public int getTipoAttivita() {
        return tipoAttivita;
    }

    public void setTipoAttivita(int tipoAttivita) {
        this.tipoAttivita = tipoAttivita;
    }

    public String getDescrizioneAttivita() {
        return descrizioneAttivita;
    }

    public void setDescrizioneAttivita(String descrizioneAttivita) {
        this.descrizioneAttivita = descrizioneAttivita;
    }

    public String getUrlCommand1() {
        return urlCommand1;
    }

    public void setUrlCommand1(String urlCommand1) {
        this.urlCommand1 = urlCommand1;
    }

    public String getStatoAttivita() {
        return statoAttivita;
    }

    public void setStatoAttivita(String statoAttivita) {
        this.statoAttivita = statoAttivita;
    }

    public String getDataIn() {
        return dataIn;
    }

    public void setDataIn(String dataIn) {
        this.dataIn = dataIn;
    }

    public String getNoteAttivita() {
        return noteAttivita;
    }

    public void setNoteAttivita(String noteAttivita) {
        this.noteAttivita = noteAttivita;
    }

    public String getUrlCommand2() {
        return urlCommand2;
    }

    public void setUrlCommand2(String urlCommand2) {
        this.urlCommand2 = urlCommand2;
    }

    public String getUrlCommand3() {
        return urlCommand3;
    }

    public void setUrlCommand3(String urlCommand3) {
        this.urlCommand3 = urlCommand3;
    }

    public String getLabelUrlCommand1() {
        return labelUrlCommand1;
    }

    public void setLabelUrlCommand1(String labelUrlCommand1) {
        this.labelUrlCommand1 = labelUrlCommand1;
    }

    public String getLabelUrlCommand2() {
        return labelUrlCommand2;
    }

    public void setLabelUrlCommand2(String labelUrlCommand2) {
        this.labelUrlCommand2 = labelUrlCommand2;
    }

    public String getLabelUrlCommand3() {
        return labelUrlCommand3;
    }

    public void setLabelUrlCommand3(String labelUrlCommand3) {
        this.labelUrlCommand3 = labelUrlCommand3;
    }

    public String getUuidAnteprima() {
        return uuidAnteprima;
    }

    public void setUuidAnteprima(String uuidAnteprima) {
        this.uuidAnteprima = uuidAnteprima;
    }

    public String getProvenienza() {
        return provenienza;
    }

    public void setProvenienza(String provenienza) {
        this.provenienza = provenienza;
    }

    public String getPriorita() {
        return priorita;
    }

    public void setPriorita(String priorita) {
        this.priorita = priorita;
    }

    public String getOggettoAttivita() {
        return oggettoAttivita;
    }

    public void setOggettoAttivita(String oggettoAttivita) {
        this.oggettoAttivita = oggettoAttivita;
    }

    public String getIdSetAttivita() {
        return idSetAttivita;
    }

    public void setIdSetAttivita(String idSetAttivita) {
        this.idSetAttivita = idSetAttivita;
    }

    public String getIdEsterno() {
        return idEsterno;
    }

    public void setIdEsterno(String idEsterno) {
        this.idEsterno = idEsterno;
    }

    public String getCustomApp1() {
        return customApp1;
    }

    public void setCustomApp1(String customApp1) {
        this.customApp1 = customApp1;
    }

    public String getCustomApp2() {
        return customApp2;
    }

    public void setCustomApp2(String customApp2) {
        this.customApp2 = customApp2;
    }

    public String getCustomApp3() {
        return customApp3;
    }

    public void setCustomApp3(String customApp3) {
        this.customApp3 = customApp3;
    }

    public String getCustomApp4() {
        return customApp4;
    }

    public void setCustomApp4(String customApp4) {
        this.customApp4 = customApp4;
    }

    public String getCustomApp5() {
        return customApp5;
    }

    public void setCustomApp5(String customApp5) {
        this.customApp5 = customApp5;
    }

    public String getPermesso() {
        return permesso;
    }

    public void setPermesso(String permesso) {
        this.permesso = permesso;
    }

    public String getIdSorgente() {
        return idSorgente;
    }

    public void setIdSorgente(String idSorgente) {
        this.idSorgente = idSorgente;
    }

    public String getTipoSorgente() {
        return tipoSorgente;
    }

    public void setTipoSorgente(String tipoSorgente) {
        this.tipoSorgente = tipoSorgente;
    }

    public String getIdRiferimento() {
        return idRiferimento;
    }

    public void setIdRiferimento(String idRiferimento) {
        this.idRiferimento = idRiferimento;
    }
    
    public String getTipoRiferimento() {
        return tipoRiferimento;
    }

    public void setTipoRiferimento(String tipoRiferimento) {
        this.tipoRiferimento = tipoRiferimento;
    }

    public String getGruppo() {
        return gruppo;
    }

    public void setGruppo(String gruppo) {
        this.gruppo = gruppo;
    }

    @JsonIgnore
    public String getJSONString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.setTimeZone(TimeZone.getDefault());
        String writeValueAsString = mapper.writeValueAsString(this);
        return writeValueAsString;
    }
}

