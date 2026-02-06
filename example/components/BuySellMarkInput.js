import React, { useState } from 'react'
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native'

const BuySellMarkInput = ({ theme, onAddBuySellMark, onRemoveBuySellMark, onUpdateBuySellMark, currentPrice, buySellMarks, klineData }) => {
	const [markType, setMarkType] = useState('buy')
	const [priceInput, setPriceInput] = useState('')
	const [amountInput, setAmountInput] = useState('1.0')
	const [updateMarkId, setUpdateMarkId] = useState('')
	const [updateType, setUpdateType] = useState('buy')
	const [updatePriceInput, setUpdatePriceInput] = useState('')

	const handleAddMark = () => {
		if (!klineData || klineData.length === 0) return

		// Use the last candlestick's time for demonstration
		const lastCandlestick = klineData[klineData.length - 10]
		const time = lastCandlestick.time

		const price = priceInput ? parseFloat(priceInput) : currentPrice
		const amount = amountInput || '1.0'

		if (price > 0) {
			onAddBuySellMark(markType, time, price, amount, 1)
			setPriceInput('')
			setAmountInput('1.0')
		}
	}

	const handleRemoveMark = () => {
		if (updateMarkId.trim()) {
			onRemoveBuySellMark(updateMarkId.trim())
			setUpdateMarkId('')
		}
	}

	const handleUpdateMark = () => {
		const price = parseFloat(updatePriceInput)
		if (!isNaN(price) && price > 0 && updateMarkId.trim()) {
			onUpdateBuySellMark(updateMarkId.trim(), updateType, price)
			setUpdateMarkId('')
			setUpdatePriceInput('')
		}
	}

	const styles = StyleSheet.create({
		container: {
			padding: 10,
			backgroundColor: theme.backgroundColor,
			borderTopWidth: 1,
			borderTopColor: theme.gridColor,
		},
		row: {
			flexDirection: 'row',
			alignItems: 'center',
			marginBottom: 8,
		},
		label: {
			color: theme.textColor,
			fontSize: 14,
			marginRight: 8,
			fontWeight: '600',
		},
		input: {
			flex: 1,
			height: 36,
			borderWidth: 1,
			borderColor: theme.gridColor,
			borderRadius: 4,
			paddingHorizontal: 8,
			backgroundColor: theme.panelBackgroundColor,
			color: theme.textColor,
			fontSize: 14,
			marginRight: 8,
		},
		shortInput: {
			width: 80,
			height: 36,
			borderWidth: 1,
			borderColor: theme.gridColor,
			borderRadius: 4,
			paddingHorizontal: 8,
			backgroundColor: theme.panelBackgroundColor,
			color: theme.textColor,
			fontSize: 14,
			marginRight: 8,
		},
		typeButton: {
			backgroundColor: theme.gridColor,
			paddingHorizontal: 12,
			paddingVertical: 8,
			borderRadius: 4,
			marginRight: 8,
		},
		typeButtonActive: {
			backgroundColor: theme.buttonColor,
		},
		typeButtonText: {
			color: theme.textColor,
			fontSize: 14,
			fontWeight: '600',
		},
		typeButtonTextActive: {
			color: theme.backgroundColor,
		},
		addButton: {
			backgroundColor: theme.buttonColor,
			paddingHorizontal: 12,
			paddingVertical: 8,
			borderRadius: 4,
		},
		addButtonText: {
			color: theme.backgroundColor,
			fontSize: 14,
			fontWeight: '600',
		},
		removeButton: {
			backgroundColor: theme.decreaseColor,
			paddingHorizontal: 12,
			paddingVertical: 8,
			borderRadius: 4,
		},
		removeButtonText: {
			color: 'white',
			fontSize: 14,
			fontWeight: '600',
		},
		updateButton: {
			backgroundColor: theme.increaseColor,
			paddingHorizontal: 12,
			paddingVertical: 8,
			borderRadius: 4,
		},
		updateButtonText: {
			color: 'white',
			fontSize: 14,
			fontWeight: '600',
		},
		markInfo: {
			color: theme.gridColor,
			fontSize: 10,
			marginTop: 4,
		},
		currentPriceButton: {
			backgroundColor: theme.gridColor,
			paddingHorizontal: 8,
			paddingVertical: 4,
			borderRadius: 4,
			marginRight: 8,
		},
		currentPriceButtonText: {
			color: theme.textColor,
			fontSize: 12,
		},
	})

	return (
		<View style={styles.container}>
			{/* Add Buy/Sell Mark Row */}
			<View>
				<Text style={styles.label}>Add Mark:</Text>
				<TouchableOpacity
					style={[styles.typeButton, markType === 'buy' && styles.typeButtonActive]}
					onPress={() => setMarkType('buy')}
				>
					<Text style={[styles.typeButtonText, markType === 'buy' && styles.typeButtonTextActive]}>
						Buy
					</Text>
				</TouchableOpacity>
				<TouchableOpacity
					style={[styles.typeButton, markType === 'sell' && styles.typeButtonActive]}
					onPress={() => setMarkType('sell')}
				>
					<Text style={[styles.typeButtonText, markType === 'sell' && styles.typeButtonTextActive]}>
						Sell
					</Text>
				</TouchableOpacity>
				<TextInput
					style={styles.shortInput}
					value={priceInput}
					onChangeText={setPriceInput}
					placeholder="Price"
					placeholderTextColor={theme.gridColor}
					keyboardType="numeric"
				/>
				<TextInput
					style={styles.shortInput}
					value={amountInput}
					onChangeText={setAmountInput}
					placeholder="Amount"
					placeholderTextColor={theme.gridColor}
				/>
				{currentPrice && (
					<TouchableOpacity
						style={styles.currentPriceButton}
						onPress={() => setPriceInput(currentPrice.toString())}
					>
						<Text style={styles.currentPriceButtonText}>
							Current: {currentPrice.toFixed(2)}
						</Text>
					</TouchableOpacity>
				)}
				<TouchableOpacity style={styles.addButton} onPress={handleAddMark}>
					<Text style={styles.addButtonText}>Add</Text>
				</TouchableOpacity>
			</View>

			{/* Update/Remove Mark Row */}
			<View style={styles.row}>
				<Text style={styles.label}>Manage Mark:</Text>
				<TouchableOpacity
					style={[styles.typeButton, updateType === 'buy' && styles.typeButtonActive]}
					onPress={() => setUpdateType('buy')}
				>
					<Text style={[styles.typeButtonText, updateType === 'buy' && styles.typeButtonTextActive]}>
						Buy
					</Text>
				</TouchableOpacity>
				<TouchableOpacity
					style={[styles.typeButton, updateType === 'sell' && styles.typeButtonActive]}
					onPress={() => setUpdateType('sell')}
				>
					<Text style={[styles.typeButtonText, updateType === 'sell' && styles.typeButtonTextActive]}>
						Sell
					</Text>
				</TouchableOpacity>
				<TextInput
					style={styles.shortInput}
					value={updateMarkId}
					onChangeText={setUpdateMarkId}
					placeholder="Mark ID"
					placeholderTextColor={theme.gridColor}
				/>
				<TextInput
					style={styles.input}
					value={updatePriceInput}
					onChangeText={setUpdatePriceInput}
					placeholder="New price"
					placeholderTextColor={theme.gridColor}
					keyboardType="numeric"
				/>
				<TouchableOpacity style={styles.updateButton} onPress={handleUpdateMark}>
					<Text style={styles.updateButtonText}>Update</Text>
				</TouchableOpacity>
				<TouchableOpacity style={styles.removeButton} onPress={handleRemoveMark}>
					<Text style={styles.removeButtonText}>Remove</Text>
				</TouchableOpacity>
			</View>

			{/* Show available mark IDs */}
			{buySellMarks && Object.keys(buySellMarks).length > 0 && (
				<Text style={styles.markInfo}>
					Available Mark IDs: {Object.keys(buySellMarks).join(', ')}
				</Text>
			)}
		</View>
	)
}

export default BuySellMarkInput