package py.com.sodep.notificationserver.business;

import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.sql.SQLException;
import java.util.Iterator;
import javapns.json.JSONException;
import javapns.notification.Payload;
import javapns.notification.PushNotificationPayload;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.hibernate.HibernateException;
import py.com.sodep.notificationserver.db.dao.AplicacionDao;
import py.com.sodep.notificationserver.db.dao.EventoDao;
import py.com.sodep.notificationserver.db.entities.Aplicacion;
import py.com.sodep.notificationserver.db.entities.Evento;
import py.com.sodep.notificationserver.db.entities.AndroidNotification;
import py.com.sodep.notificationserver.db.entities.AndroidResponse;
import py.com.sodep.notificationserver.db.entities.IosResponse;
import py.com.sodep.notificationserver.exceptions.handlers.BusinessException;
import py.com.sodep.notificationserver.exceptions.handlers.ExceptionMapperHelper;
import py.com.sodep.notificationserver.facade.ApnsFacade;
import py.com.sodep.notificationserver.facade.GcmFacade;

@ApplicationScoped
public class NotificationBusiness {

    @Inject
    AplicacionDao appDao;
    @Inject
    ApnsFacade facade;
    @Inject
    GcmFacade service;
    @Inject
    AndroidNotification notification;
    @Inject
    EventoDao eventoDao;
    @Inject
    Logger logger;

    public Evento crearEvento(Evento e, String appName) throws BusinessException, HibernateException, SQLException {
        Aplicacion a = appDao.getByName(appName);
        if (a != null) {
            e.setAplicacion(a);
            validate(e);
            eventoDao.create(e);
            return e;
        } else {
            throw new BusinessException(ExceptionMapperHelper.appError.APLICACION_NOT_FOUND.ordinal(), "La aplicacion " + appName + " no existe.");
        }
    }

    public Evento actualizarEvento(Evento e) throws HibernateException, SQLException {
        eventoDao.update(e);
        return e;
    }

    public Evento notificar(Evento e) throws BusinessException, HibernateException, SQLException {
        Aplicacion app = appDao.getByName(e.getAplicacion().getNombre());
        if (app != null) {
            if (e.isProductionMode()) {
                if (app.getApiKeyProd() != null) {
                    e.setAndroidResponse(notificarAndroid(app.getApiKeyProd(), e));
                }
                if (app.getCertificadoProd() != null && app.getKeyFileProd() != null) {
                    e.setIosResponse(notificarIos(app.getCertificadoProd(), app.getKeyFileProd(), e, true));
                }
            } else {
                if (app.getApiKeyDev() != null) {
                    e.setAndroidResponse(notificarAndroid(app.getApiKeyDev(), e));
                }
                if (app.getCertificadoDev() != null && app.getKeyFileDev() != null) {
                    e.setIosResponse(notificarIos(app.getCertificadoDev(), app.getKeyFileDev(), e, false));
                }
            }
        } else {
            throw new BusinessException(ExceptionMapperHelper.appError.APLICACION_NOT_FOUND.ordinal(), "La aplicacion " + e.getAplicacion().getNombre() + " no existe.");
        }
        e.setEstadoAndroid("ENVIADO");
        e.setEstadoIos("ENVIADO");
        return e;
    }

    @SuppressWarnings("rawtypes")
    public IosResponse notificarIos(String certifadoPath, String keyFile,
            Evento evento, Boolean productionMode) throws BusinessException, HibernateException, SQLException {
        logger.info("[Evento: " + evento.getId() + "]: Notificando iOs");
        File certificado = new File(certifadoPath);
        Payload payload = PushNotificationPayload.complex();
        ObjectNode pay = evento.getObjectNodePayLoad();
        try {
            ((PushNotificationPayload) payload).addAlert(evento
                    .getDescripcion());

            ((PushNotificationPayload) payload).addSound("default");

            Iterator it = pay.fieldNames();
            while (it.hasNext()) {
                String pair = (String) it.next();
                logger.info(pair + " = " + pay.get(pair));
                payload.addCustomDictionary((String) pair,
                        pay.get(pair).asText());
            }
        } catch (JSONException e) {
            throw new BusinessException(ExceptionMapperHelper.appError.BAD_REQUEST.ordinal(), "Error al parsear payload en notificacion iOs.");
        }
        return facade.send(payload, certificado, keyFile, productionMode, evento.getIosDevicesList());

    }

    public AndroidResponse notificarAndroid(String apiKey, Evento evento) throws BusinessException, HibernateException, SQLException {

        logger.info("[Evento: " + evento.getId() + "]: notificando android");
        if (evento.getAndroidDevicesList().size() == 1) {
            logger.info("[Evento: " + evento.getId() + "]: Un solo device. Notificando android");
            notification.setTo(evento.getAndroidDevicesList().get(0));
        } else {
            logger.info("[Evento: " + evento.getId() + "]: Lista. Notificando android");
            notification.setRegistration_ids(evento.getAndroidDevicesList());
        }
        notification.setData(evento.getObjectNodePayLoad());
        return service.send(apiKey, notification);
    }

    public void validate(Evento e) throws BusinessException {
        String s = e.getDescripcion() + e.getPayload().asText();
        if(s.getBytes().length > e.getAplicacion().getPayloadSize()){
            throw new BusinessException(500, "El tamaño del payload supera el configurado para la aplicación: " + e.getAplicacion().getPayloadSize());
        }
    }
}
