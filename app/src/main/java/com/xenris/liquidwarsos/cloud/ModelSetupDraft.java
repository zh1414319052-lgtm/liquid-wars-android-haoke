// SPDX-License-Identifier: GPL-3.0-or-later
package com.xenris.liquidwarsos.cloud;
import java.util.*;

/** UI-thread-owned draft. Kept only in RAM; never put API keys into Bundle or preferences. */
public final class ModelSetupDraft {
    private ModelConfig value;
    private long revision;
    private boolean dirty;
    private List<String> models=new ArrayList<>();
    public ModelSetupDraft(ModelConfig saved){
        List<String> defaults=ModelProviders.defaults(saved);
        value=new ModelConfig(saved.enabled,saved.protocol,saved.base,saved.model.isEmpty()?ModelProviders.recommend(defaults):saved.model,saved.key);
        models.addAll(defaults);if(!value.model.isEmpty()&&!models.contains(value.model))models.add(0,value.model);
    }
    public ModelConfig value(){return value;}
    public long revision(){return revision;}
    public boolean dirty(){return dirty;}
    public List<String> models(){return new ArrayList<>(models);}
    public void edit(ModelConfig next){
        if(same(value,next))return;
        boolean endpoint=!value.base.equals(next.base)||value.protocol!=next.protocol;
        boolean keyChanged=!value.key.equals(next.key);
        if(endpoint)next=new ModelConfig(next.enabled,next.protocol,next.base,next.model,"");
        if(endpoint||keyChanged){models=ModelProviders.defaults(next);if(!next.model.isEmpty()&&!models.contains(next.model))models.add(0,next.model);}
        value=next;revision++;dirty=true;
    }
    public void provider(int index){
        ModelProviders.Provider p=ModelProviders.ALL[index];
        boolean same=p.base.equals(value.base)&&p.protocol==value.protocol;
        edit(new ModelConfig(true,p.protocol,p.base,ModelProviders.recommend(Arrays.asList(p.defaults)),same?value.key:""));
        models=new ArrayList<>(Arrays.asList(p.defaults));
    }
    public void model(String id){edit(new ModelConfig(value.enabled,value.protocol,value.base,id,value.key));}
    public boolean directory(long expected,List<String> result){
        if(expected!=revision)return false;
        models=new ArrayList<>(result);
        if(value.model.isEmpty())model(ModelProviders.recommend(models));
        return true;
    }
    public void saved(){dirty=false;revision++;}
    public static boolean same(ModelConfig a,ModelConfig b){return a.enabled==b.enabled&&a.protocol==b.protocol&&a.base.equals(b.base)&&a.model.equals(b.model)&&a.key.equals(b.key);}
}
