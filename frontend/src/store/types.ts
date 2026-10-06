// Shapes of the online shop's public API (/api/shop), see ShopDtos.java

export type Availability = 'IN_STOCK' | 'FEW_LEFT' | 'OUT_OF_STOCK'

export interface CategoryLink {
  id: number
  slug: string
  name: string
}

export interface ShopCategory extends CategoryLink {
  productCount: number
  imageUrl: string | null
}

export interface ShopProduct {
  id: number
  slug: string
  name: string
  category: CategoryLink | null
  price: number
  // Struck through for 30 days after a price drop
  previousPrice: number | null
  availability: Availability
  // Only when few are left
  onlyLeft: number | null
  // The most a visitor can put in the cart
  maxQuantity: number
  imageUrl: string | null
}

export interface ShopProductDetail extends Omit<ShopProduct, 'imageUrl'> {
  description: string | null
  images: { id: number; url: string; width: number; height: number }[]
}

export interface ShopHome {
  categories: ShopCategory[]
  deals: ShopProduct[]
  bestSellers: ShopProduct[]
}

// "/p/1-galaxy-s26": the id finds the product, the name makes the address readable
export const productPath = (product: { id: number; slug: string }) => `/p/${product.id}-${product.slug}`
