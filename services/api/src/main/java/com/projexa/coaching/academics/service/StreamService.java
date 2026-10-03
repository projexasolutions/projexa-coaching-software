package com.projexa.coaching.academics.service;

import com.projexa.coaching.common.exceptions.ApiException;
import com.projexa.coaching.common.tenant.TenantContextHolder;
import com.projexa.coaching.academics.entity.Stream;
import com.projexa.coaching.academics.repository.StreamRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class StreamService {
    private final StreamRepository repo;
    public StreamService(StreamRepository repo){this.repo=repo;}
    public List<Stream> list(){return repo.findAllByTenantId(TenantContextHolder.getRequired());}
    public Stream get(UUID id){return repo.findByIdAndTenantId(id,TenantContextHolder.getRequired()).orElseThrow(()->new ApiException("NOT_FOUND","Stream not found"));}
    public Stream create(Stream item){validate(item);item.setTenantId(TenantContextHolder.getRequired());return repo.save(item);}
    public Stream update(UUID id,Stream item){validate(item);Stream current=get(id);copy(item,current);return repo.save(current);}
    public void delete(UUID id){Stream current=get(id);current.setActive(false);repo.save(current);}
    private void validate(Stream x){
        if(x==null||x.getName()==null||x.getName().trim().isEmpty())throw new ApiException("VALIDATION_ERROR","Stream name is required");
        x.setName(x.getName().trim());
        if(x.getCode()!=null){String c=x.getCode().trim();x.setCode(c.isEmpty()?null:c.toUpperCase());}
    }
    private void copy(Stream s,Stream t){t.setName(s.getName());t.setCode(s.getCode());t.setActive(s.isActive());}
}
