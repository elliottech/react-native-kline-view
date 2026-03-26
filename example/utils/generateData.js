export const generateMockData = () => {
  const data = []
  let lastClose = 50000
  const now = Date.now()
  
  for (let i = 0; i < 200; i++) {
    const time = now - (200 - i) * 1 * 60 * 1000 // 15-minute interval
    
    // Next open equals previous close, ensuring continuity
    const open = lastClose
    
    // Generate reasonable high and low prices
    const volatility = 0.02 // 2% volatility
    const change = (Math.random() - 0.5) * open * volatility
    const close = Math.max(open + change, open * 0.95) // Maximum decline 5%
    
    // Ensure high >= max(open, close), low <= min(open, close)
    const maxPrice = Math.max(open, close)
    const minPrice = Math.min(open, close)
    const high = maxPrice + Math.random() * open * 0.01 // Max 1% higher
    const low = minPrice - Math.random() * open * 0.01 // Max 1% lower
    
    const volume = (0.5 + Math.random()) * 1000000 // Volume from 500K to 1.5M
    
    data.push({
      time: time,
      open: parseFloat(open.toFixed(2)),
      high: parseFloat(high.toFixed(2)),
      low: parseFloat(low.toFixed(2)),
      close: parseFloat(close.toFixed(2)),
      vol: parseFloat(volume.toFixed(2))  // Native code expects 'vol' not 'volume'
    })

    // TO TEST EMPTY CANDLESTICKS UNCOMMENT THIS
    // if (i % 5 === 0) {
    //   data[data.length - 1].vol = 0 // Simulate occasional volume spikes
    //   data[data.length - 1].close = data[data.length - 1].open // Simulate occasional price stability
    //   data[data.length - 1].high = data[data.length - 1].open // Simulate occasional price stability
    //   data[data.length - 1].low = data[data.length - 1].open // Simulate occasional price stability
    // }
    
    lastClose = close
  }
  
  return data
}

export const generateMoreHistoricalData = (existingData, count = 200) => {
  const newData = []
  const firstItem = existingData[0]
  let lastClose = firstItem.open

  for (let i = count; i > 0; i--) {
    const time = firstItem.time - (count-i+1) * 1 * 60 * 1000 // 15-minute interval, pushing backward

    const open = lastClose
    const volatility = 0.02
    const change = (Math.random() - 0.5) * open * volatility
    const close = Math.max(open + change, open * 0.95)

    const maxPrice = Math.max(open, close)
    const minPrice = Math.min(open, close)
    const high = maxPrice + Math.random() * open * 0.01
    const low = minPrice - Math.random() * open * 0.01

    const volume = (0.5 + Math.random()) * 1000000

    newData.push({
      time: time,
      open: parseFloat(open.toFixed(2)),
      high: parseFloat(high.toFixed(2)),
      low: parseFloat(low.toFixed(2)),
      close: parseFloat(close.toFixed(2)),
      vol: parseFloat(volume.toFixed(2))  // Native code expects 'vol' not 'volume'
    })

    lastClose = close
  }

  newData.reverse() // Ensure chronological order

  return newData
}