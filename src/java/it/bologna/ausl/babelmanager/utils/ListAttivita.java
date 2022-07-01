package it.bologna.ausl.babelmanager.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 *
 * @author utente
 */
public class ListAttivita {

    private List<Attivita> listAttivita;

    public List<Attivita> getListAttivita() {
        return listAttivita;
    }

    public void setListAttivita(List<Attivita> listAttivita) {
        this.listAttivita = listAttivita;
    }
    
    @JsonIgnore
    public void addAttivita(Attivita attivita) {
        if (listAttivita == null)
            listAttivita = new ArrayList<>();
        listAttivita.add(attivita);
    }
    
    @JsonIgnore
    public Attivita getAttivita(int index){
        return listAttivita.get(index);
    }

    @JsonIgnore
    public int getSize(){
        return listAttivita.size();
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
