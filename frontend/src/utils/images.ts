// Shrinks a picture in the browser before it's uploaded: a 5 MB phone photo becomes a JPEG of
// about 150 KB, upright (the browser applies the photo's rotation), without its hidden data
// (GPS position...). The server checks and re-encodes it anyway.
const MAX_SIDE = 1200

export async function prepareImage(file: File): Promise<Blob> {
  let bitmap: ImageBitmap
  try {
    bitmap = await createImageBitmap(file)
  } catch {
    throw new Error(`${file.name} isn't a picture the browser can open`)
  }
  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height))
  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(bitmap.width * scale))
  canvas.height = Math.max(1, Math.round(bitmap.height * scale))
  const context = canvas.getContext('2d')!
  // White behind transparent parts, as in the JPEG the server keeps
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  bitmap.close()
  return new Promise((resolve, reject) =>
    canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Could not prepare the picture'))), 'image/jpeg', 0.88))
}
