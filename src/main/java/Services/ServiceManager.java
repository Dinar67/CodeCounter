package Services;

import Interfaces.IService;

import java.util.HashMap;

public class ServiceManager {
    private HashMap<Class<? extends IService>, IService> _services = new HashMap<>();

    public <T extends IService> T setService(Class<T> serviceClass, IService service){
        _services.put(serviceClass, service);
        return serviceClass.cast(service);
    }

    public <T extends IService> T getService(Class<T> serviceClass){
        return serviceClass.cast(_services.get(serviceClass));
    }
}
