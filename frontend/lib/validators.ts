import { z } from 'zod';

export const envioSchema = z
  .object({
    // trackingCtg: z
    //   .string()
    //   .min(1, 'El CTG es requerido')
    //   .regex(/^\d+$/, 'El CTG debe contener solo numeros'),
    cpe: z
      .string()
      .min(1, 'El CPE es requerido'),
    clienteCuit: z
      .string()
      .min(1, 'Seleccione un cliente'),
    idOrigen: z
      .number({ invalid_type_error: 'Seleccione un origen' })
      .positive('Seleccione un origen'),
    idDestino: z
      .number({ invalid_type_error: 'Seleccione un destino' })
      .positive('Seleccione un destino'),
    tipoGrano: z
      .string()
      .min(1, 'Seleccione un tipo de grano'),
    kgOrigen: z
      .number({ invalid_type_error: 'Ingrese el peso' })
      .positive('El peso debe ser mayor a 0'),
    // NUEVO CAMPO: Validación estricta para el término legal
    aceptaTerminos: z.boolean().refine((val) => val === true, {
      message: 'Debe aceptar el tratamiento de datos para continuar',
    }),
  })
  .refine((data) => data.idOrigen !== data.idDestino, {
    message: 'El origen y destino deben ser diferentes',
    path: ['idDestino'],
  });

export type EnvioFormData = z.infer<typeof envioSchema>;