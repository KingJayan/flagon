/** the page half of javax.imageio: the browser decodes the formats java does not */

type Decoded = (
  error: string | null,
  width: number,
  height: number,
  rgba: Int8Array | null,
) => void;

export interface Decoder {
  decode(file: Int8Array<ArrayBuffer>, done: Decoded): void;
}

declare global {
  var __flagonImages: Decoder | undefined;
}

async function decode(file: Int8Array<ArrayBuffer>): Promise<ImageData> {
  const bitmap = await createImageBitmap(new Blob([file]), {
    premultiplyAlpha: 'none',
    colorSpaceConversion: 'none',
  });
  try {
    const canvas = new OffscreenCanvas(bitmap.width, bitmap.height);
    const context = canvas.getContext('2d', { willReadFrequently: true });
    if (context === null) {
      throw new Error('this browser gives no 2d context to decode into');
    }
    context.drawImage(bitmap, 0, 0);
    return context.getImageData(0, 0, bitmap.width, bitmap.height);
  } finally {
    bitmap.close();
  }
}

export function decoder(): Decoder {
  return {
    decode(file, done): void {
      decode(file).then(
        (image) =>
          done(
            null,
            image.width,
            image.height,
            new Int8Array(image.data.buffer, image.data.byteOffset, image.data.length),
          ),
        (error: unknown) =>
          done(error instanceof Error ? error.message : String(error), 0, 0, null),
      );
    },
  };
}

export function installImages(): void {
  globalThis.__flagonImages ??= decoder();
}
