import { z } from 'zod';

export const envioSchema = z
  .object({
    // tracking_ctg: z
    //   .string()
    //   .min(1, 'El CTG es requerido')
    //   .regex(/^\d+$/, 'El CTG debe contener solo numeros'),
    cpe: z
      .string()
      .min(1, 'El CPE es requerido'),
    clienteCuit: z
      .string()
      .min(1, 'Seleccione un cliente'),
    id_origen: z
      .number({ invalid_type_error: 'Seleccione un origen' })
      .positive('Seleccione un origen'),
    id_destino: z
      .number({ invalid_type_error: 'Seleccione un destino' })
      .positive('Seleccione un destino'),
    tipo_grano: z
      .string()
      .min(1, 'Seleccione un tipo de grano'),
    kg_origen: z
      .number({ invalid_type_error: 'Ingrese el peso' })
      .positive('El peso debe ser mayor a 0'),
    // NUEVO CAMPO: Validación estricta para el término legal
    acepta_terminos: z.boolean().refine((val) => val === true, {
      message: 'Debe aceptar el tratamiento de datos para continuar',
    }),
  })
  .refine((data) => data.id_origen !== data.id_destino, {
    message: 'El origen y destino deben ser diferentes',
    path: ['id_destino'],
  });

export type EnvioFormData = z.infer<typeof envioSchema>;