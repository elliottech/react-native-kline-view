import React, { useState } from 'react'
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native'

const OrderInput = ({ theme, onAddOrder, onUpdateOrder, currentPrice, orderLines }) => {
	const [priceInput, setPriceInput] = useState(currentPrice ? currentPrice.toString() : '')
	const [labelInput, setLabelInput] = useState('')
	const [updateOrderId, setUpdateOrderId] = useState('')
	const [updatePriceInput, setUpdatePriceInput] = useState('')

	const handleAddOrder = () => {
		const price = parseFloat(priceInput)
		if (!isNaN(price) && price > 0) {
			onAddOrder(price, labelInput)
			setLabelInput('')
		}
	}

	const handleUpdateOrder = () => {
		const price = parseFloat(updatePriceInput)
		if (!isNaN(price) && price > 0 && updateOrderId.trim()) {
			onUpdateOrder(updateOrderId.trim(), price)
			setUpdateOrderId('')
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
		orderInfo: {
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
			{/* Add Order Row */}
			<View style={styles.row}>
				<Text style={styles.label}>Add Order:</Text>
				<TextInput
					style={styles.shortInput}
					value={priceInput}
					onChangeText={setPriceInput}
					placeholder="Price"
					placeholderTextColor={theme.gridColor}
					keyboardType="numeric"
				/>
				<TextInput
					style={styles.input}
					value={labelInput}
					onChangeText={setLabelInput}
					placeholder="Label (optional)"
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
				<TouchableOpacity style={styles.addButton} onPress={handleAddOrder}>
					<Text style={styles.addButtonText}>Add</Text>
				</TouchableOpacity>
			</View>

			{/* Update Order Row */}
			<View style={styles.row}>
				<Text style={styles.label}>Update Order:</Text>
				<TextInput
					style={styles.shortInput}
					value={updateOrderId}
					onChangeText={setUpdateOrderId}
					placeholder="Order ID"
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
				<TouchableOpacity style={styles.updateButton} onPress={handleUpdateOrder}>
					<Text style={styles.updateButtonText}>Update</Text>
				</TouchableOpacity>
			</View>

			{/* Show available order IDs */}
			{orderLines && Object.keys(orderLines).length > 0 && (
				<Text style={styles.orderInfo}>
					Available Order IDs: {Object.keys(orderLines).join(', ')}
				</Text>
			)}
		</View>
	)
}

export default OrderInput