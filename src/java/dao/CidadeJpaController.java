/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import dao.exceptions.IllegalOrphanException;
import dao.exceptions.NonexistentEntityException;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import model.Estado;
import model.Fornecedor;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import model.Cidade;

/**
 *
 * @author aluno
 */
public class CidadeJpaController implements Serializable {

    public CidadeJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Cidade cidade) {
        if (cidade.getFornecedorList() == null) {
            cidade.setFornecedorList(new ArrayList<Fornecedor>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Estado estadoId = cidade.getEstadoId();
            if (estadoId != null) {
                estadoId = em.getReference(estadoId.getClass(), estadoId.getId());
                cidade.setEstadoId(estadoId);
            }
            List<Fornecedor> attachedFornecedorList = new ArrayList<Fornecedor>();
            for (Fornecedor fornecedorListFornecedorToAttach : cidade.getFornecedorList()) {
                fornecedorListFornecedorToAttach = em.getReference(fornecedorListFornecedorToAttach.getClass(), fornecedorListFornecedorToAttach.getId());
                attachedFornecedorList.add(fornecedorListFornecedorToAttach);
            }
            cidade.setFornecedorList(attachedFornecedorList);
            em.persist(cidade);
            if (estadoId != null) {
                estadoId.getCidadeList().add(cidade);
                estadoId = em.merge(estadoId);
            }
            for (Fornecedor fornecedorListFornecedor : cidade.getFornecedorList()) {
                Cidade oldCidadeIdOfFornecedorListFornecedor = fornecedorListFornecedor.getCidadeId();
                fornecedorListFornecedor.setCidadeId(cidade);
                fornecedorListFornecedor = em.merge(fornecedorListFornecedor);
                if (oldCidadeIdOfFornecedorListFornecedor != null) {
                    oldCidadeIdOfFornecedorListFornecedor.getFornecedorList().remove(fornecedorListFornecedor);
                    oldCidadeIdOfFornecedorListFornecedor = em.merge(oldCidadeIdOfFornecedorListFornecedor);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Cidade cidade) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Cidade persistentCidade = em.find(Cidade.class, cidade.getId());
            Estado estadoIdOld = persistentCidade.getEstadoId();
            Estado estadoIdNew = cidade.getEstadoId();
            List<Fornecedor> fornecedorListOld = persistentCidade.getFornecedorList();
            List<Fornecedor> fornecedorListNew = cidade.getFornecedorList();
            List<String> illegalOrphanMessages = null;
            for (Fornecedor fornecedorListOldFornecedor : fornecedorListOld) {
                if (!fornecedorListNew.contains(fornecedorListOldFornecedor)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Fornecedor " + fornecedorListOldFornecedor + " since its cidadeId field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (estadoIdNew != null) {
                estadoIdNew = em.getReference(estadoIdNew.getClass(), estadoIdNew.getId());
                cidade.setEstadoId(estadoIdNew);
            }
            List<Fornecedor> attachedFornecedorListNew = new ArrayList<Fornecedor>();
            for (Fornecedor fornecedorListNewFornecedorToAttach : fornecedorListNew) {
                fornecedorListNewFornecedorToAttach = em.getReference(fornecedorListNewFornecedorToAttach.getClass(), fornecedorListNewFornecedorToAttach.getId());
                attachedFornecedorListNew.add(fornecedorListNewFornecedorToAttach);
            }
            fornecedorListNew = attachedFornecedorListNew;
            cidade.setFornecedorList(fornecedorListNew);
            cidade = em.merge(cidade);
            if (estadoIdOld != null && !estadoIdOld.equals(estadoIdNew)) {
                estadoIdOld.getCidadeList().remove(cidade);
                estadoIdOld = em.merge(estadoIdOld);
            }
            if (estadoIdNew != null && !estadoIdNew.equals(estadoIdOld)) {
                estadoIdNew.getCidadeList().add(cidade);
                estadoIdNew = em.merge(estadoIdNew);
            }
            for (Fornecedor fornecedorListNewFornecedor : fornecedorListNew) {
                if (!fornecedorListOld.contains(fornecedorListNewFornecedor)) {
                    Cidade oldCidadeIdOfFornecedorListNewFornecedor = fornecedorListNewFornecedor.getCidadeId();
                    fornecedorListNewFornecedor.setCidadeId(cidade);
                    fornecedorListNewFornecedor = em.merge(fornecedorListNewFornecedor);
                    if (oldCidadeIdOfFornecedorListNewFornecedor != null && !oldCidadeIdOfFornecedorListNewFornecedor.equals(cidade)) {
                        oldCidadeIdOfFornecedorListNewFornecedor.getFornecedorList().remove(fornecedorListNewFornecedor);
                        oldCidadeIdOfFornecedorListNewFornecedor = em.merge(oldCidadeIdOfFornecedorListNewFornecedor);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = cidade.getId();
                if (findCidade(id) == null) {
                    throw new NonexistentEntityException("The cidade with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Cidade cidade;
            try {
                cidade = em.getReference(Cidade.class, id);
                cidade.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The cidade with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            List<Fornecedor> fornecedorListOrphanCheck = cidade.getFornecedorList();
            for (Fornecedor fornecedorListOrphanCheckFornecedor : fornecedorListOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Cidade (" + cidade + ") cannot be destroyed since the Fornecedor " + fornecedorListOrphanCheckFornecedor + " in its fornecedorList field has a non-nullable cidadeId field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Estado estadoId = cidade.getEstadoId();
            if (estadoId != null) {
                estadoId.getCidadeList().remove(cidade);
                estadoId = em.merge(estadoId);
            }
            em.remove(cidade);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Cidade> findCidadeEntities() {
        return findCidadeEntities(true, -1, -1);
    }

    public List<Cidade> findCidadeEntities(int maxResults, int firstResult) {
        return findCidadeEntities(false, maxResults, firstResult);
    }

    private List<Cidade> findCidadeEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Cidade.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Cidade findCidade(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Cidade.class, id);
        } finally {
            em.close();
        }
    }

    public int getCidadeCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Cidade> rt = cq.from(Cidade.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
