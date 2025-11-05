// ==============================================================================
// FILE: mongo.js
// ==============================================================================
// MongoDB Schema for RiverFlow Mindmap (ReactFlow Compatible)
// Version: 3.0 (ReactFlow Optimized)
// Description: Optimized for ReactFlow - nodes, edges, collaboration, real-time
// ==============================================================================

const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// ==============================================================================
// SUB-SCHEMAS (Embedded Documents)
// ==============================================================================

/**
 * Node Schema - Compatible with ReactFlow
 * ReactFlow format: { id, type, position: {x, y}, data: {...}, style, ... }
 */
const nodeSchema = new Schema({
  id: {
    type: String,
    required: true,
    description: 'Unique node identifier (UUID)'
  },
  type: {
    type: String,
    default: 'default',
    description: 'ReactFlow node type: default, input, output, or custom'
  },
  position: {
    x: { type: Number, required: true, default: 0 },
    y: { type: Number, required: true, default: 0 }
  },
  data: {
    type: Schema.Types.Mixed,
    required: true,
    description: 'Node data object (label, content, icons, etc.)'
    // Typical structure:
    // {
    //   label: 'Node text',
    //   content: 'Full content',
    //   icon: 'icon-name',
    //   color: '#fff',
    //   backgroundColor: '#000',
    //   metadata: {...}
    // }
  },
  // Optional ReactFlow fields
  style: {
    type: Schema.Types.Mixed,
    description: 'CSS style object for the node'
  },
  className: {
    type: String,
    description: 'CSS class name'
  },
  draggable: {
    type: Boolean,
    default: true
  },
  selectable: {
    type: Boolean,
    default: true
  },
  connectable: {
    type: Boolean,
    default: true
  },
  deletable: {
    type: Boolean,
    default: true
  },
  dragHandle: {
    type: String,
    description: 'CSS selector for drag handle'
  },
  // Custom fields for mindmap features
  parentNode: {
    type: String,
    description: 'Parent node ID for grouping'
  },
  extent: {
    type: String,
    enum: ['parent'],
    description: 'Node movement extent'
  },
  expandParent: {
    type: Boolean,
    default: false
  },
  // Mindmap-specific metadata
  metadata: {
    createdBy: { type: Number, description: 'MySQL user ID' },
    createdAt: { type: Date, default: Date.now },
    updatedAt: { type: Date, default: Date.now }
  }
}, { _id: false });

/**
 * Edge Schema - Compatible with ReactFlow
 * ReactFlow format: { id, source, target, type, animated, label, style, ... }
 */
const edgeSchema = new Schema({
  id: {
    type: String,
    required: true,
    description: 'Unique edge identifier'
  },
  source: {
    type: String,
    required: true,
    description: 'Source node ID'
  },
  target: {
    type: String,
    required: true,
    description: 'Target node ID'
  },
  sourceHandle: {
    type: String,
    description: 'Source handle ID (for multiple connection points)'
  },
  targetHandle: {
    type: String,
    description: 'Target handle ID'
  },
  type: {
    type: String,
    default: 'default',
    description: 'ReactFlow edge type: default, straight, step, smoothstep, or custom'
  },
  animated: {
    type: Boolean,
    default: false
  },
  label: {
    type: String,
    description: 'Edge label text'
  },
  labelStyle: {
    type: Schema.Types.Mixed,
    description: 'CSS style for label'
  },
  labelShowBg: {
    type: Boolean,
    default: true
  },
  labelBgStyle: {
    type: Schema.Types.Mixed,
    description: 'CSS style for label background'
  },
  labelBgPadding: {
    type: [Number],
    description: 'Label background padding [x, y]'
  },
  labelBgBorderRadius: {
    type: Number,
    description: 'Label background border radius'
  },
  style: {
    type: Schema.Types.Mixed,
    description: 'CSS style object for the edge'
  },
  markerStart: {
    type: Schema.Types.Mixed,
    description: 'Marker at the start of the edge'
  },
  markerEnd: {
    type: Schema.Types.Mixed,
    description: 'Marker at the end of the edge'
  },
  // Custom fields
  data: {
    type: Schema.Types.Mixed,
    description: 'Custom edge data'
  },
  // Mindmap-specific metadata
  metadata: {
    createdBy: { type: Number, description: 'MySQL user ID' },
    createdAt: { type: Date, default: Date.now }
  }
}, { _id: false });

/**
 * Collaborator Schema
 */
const collaboratorSchema = new Schema({
  mysqlUserId: {
    type: Number,
    required: true,
    description: 'MySQL user ID'
  },
  role: {
    type: String,
    enum: ['owner', 'editor', 'viewer'],
    required: true
  },
  invitedBy: {
    type: Number,
    required: true
  },
  invitedAt: {
    type: Date,
    default: Date.now
  },
  acceptedAt: {
    type: Date,
    default: null
  },
  status: {
    type: String,
    enum: ['pending', 'accepted', 'rejected', 'removed'],
    default: 'pending'
  }
}, { _id: false });

// ==============================================================================
// MAIN MINDMAP SCHEMA
// ==============================================================================

const MindmapSchema = new Schema({
  mysqlUserId: {
    type: Number,
    required: true,
    index: true,
    description: 'Owner user ID from MySQL'
  },
  title: {
    type: String,
    required: true,
    trim: true,
    maxlength: 255,
    index: true
  },
  description: {
    type: String,
    trim: true,
    maxlength: 2000
  },
  thumbnail: {
    type: String,
    description: 'URL to thumbnail'
  },
  
  // ReactFlow data
  nodes: {
    type: [nodeSchema],
    default: [],
    description: 'ReactFlow nodes array'
  },
  edges: {
    type: [edgeSchema],
    default: [],
    description: 'ReactFlow edges array'
  },
  
  // ReactFlow viewport settings
  viewport: {
    x: { type: Number, default: 0 },
    y: { type: Number, default: 0 },
    zoom: { type: Number, default: 1, min: 0.1, max: 4 }
  },
  
  // Canvas settings
  settings: {
    fitView: { type: Boolean, default: true },
    snapToGrid: { type: Boolean, default: false },
    snapGrid: {
      type: [Number],
      default: [15, 15],
      description: 'Grid size [x, y]'
    },
    nodesDraggable: { type: Boolean, default: true },
    nodesConnectable: { type: Boolean, default: true },
    elementsSelectable: { type: Boolean, default: true },
    panOnDrag: { type: Boolean, default: true },
    panOnScroll: { type: Boolean, default: false },
    zoomOnScroll: { type: Boolean, default: true },
    zoomOnPinch: { type: Boolean, default: true },
    zoomOnDoubleClick: { type: Boolean, default: true },
    defaultEdgeOptions: {
      type: Schema.Types.Mixed,
      description: 'Default edge configuration'
    },
    connectionMode: {
      type: String,
      enum: ['strict', 'loose'],
      default: 'strict'
    }
  },
  
  // Sharing & Collaboration
  isPublic: {
    type: Boolean,
    default: false,
    index: true
  },
  shareToken: {
    type: String,
    unique: true,
    sparse: true
  },
  collaborators: {
    type: [collaboratorSchema],
    default: []
  },
  
  // Organization
  tags: [{ type: String, trim: true }],
  category: {
    type: String,
    enum: ['work', 'personal', 'education', 'project', 'brainstorming', 'ai-generated', 'other'],
    default: 'other'
  },
  isFavorite: {
    type: Boolean,
    default: false
  },
  isTemplate: {
    type: Boolean,
    default: false
  },
  status: {
    type: String,
    enum: ['active', 'archived', 'deleted'],
    default: 'active',
    index: true
  },
  
  // AI Integration
  aiGenerated: {
    type: Boolean,
    default: false
  },
  aiWorkflowId: {
    type: Number,
    description: 'MySQL AI workflow ID'
  },
  aiMetadata: {
    type: Schema.Types.Mixed
  },
  
  // Metadata
  metadata: {
    nodeCount: { type: Number, default: 0 },
    edgeCount: { type: Number, default: 0 },
    lastEditedBy: { type: Number },
    viewCount: { type: Number, default: 0 },
    forkCount: { type: Number, default: 0 },
    forkedFrom: { type: Schema.Types.ObjectId, ref: 'Mindmap' }
  }
}, {
  timestamps: true,
  collection: 'mindmaps'
});

// Indexes
MindmapSchema.index({ mysqlUserId: 1, status: 1 });
MindmapSchema.index({ title: 'text', description: 'text' });
MindmapSchema.index({ tags: 1 });
MindmapSchema.index({ isPublic: 1, status: 1 });
MindmapSchema.index({ 'collaborators.mysqlUserId': 1 });
MindmapSchema.index({ createdAt: -1 });
MindmapSchema.index({ updatedAt: -1 });
MindmapSchema.index({ aiWorkflowId: 1 });
MindmapSchema.index({ category: 1 });

// Virtuals
MindmapSchema.virtual('collaboratorCount').get(function() {
  return this.collaborators.filter(c => c.status === 'accepted').length;
});

// Pre-save hook
MindmapSchema.pre('save', function(next) {
  this.metadata.nodeCount = this.nodes.length;
  this.metadata.edgeCount = this.edges.length;
  next();
});

// ==============================================================================
// HISTORY SCHEMA
// ==============================================================================

const MindmapHistorySchema = new Schema({
  mindmapId: {
    type: Schema.Types.ObjectId,
    required: true,
    ref: 'Mindmap',
    index: true
  },
  mysqlUserId: {
    type: Number,
    required: true,
    index: true
  },
  action: {
    type: String,
    enum: [
      'create', 'update', 'delete',
      'node_add', 'node_update', 'node_delete', 'node_move',
      'edge_add', 'edge_update', 'edge_delete',
      'viewport_change', 'settings_update',
      'collaborator_add', 'collaborator_remove',
      'restore', 'ai_generate'
    ],
    required: true,
    index: true
  },
  changes: {
    type: Schema.Types.Mixed,
    description: 'Delta/diff of changes'
  },
  snapshot: {
    type: Schema.Types.Mixed,
    description: 'Periodic full snapshot'
  },
  metadata: {
    ip: { type: String },
    userAgent: { type: String },
    sessionId: { type: String }
  }
}, {
  timestamps: { createdAt: true, updatedAt: false },
  collection: 'mindmap_history'
});

MindmapHistorySchema.index({ mindmapId: 1, createdAt: -1 });
MindmapHistorySchema.index({ mysqlUserId: 1, createdAt: -1 });
MindmapHistorySchema.index({ createdAt: 1 }, { expireAfterSeconds: 7776000 }); // 90 days

// ==============================================================================
// VERSION SCHEMA
// ==============================================================================

const MindmapVersionSchema = new Schema({
  mindmapId: {
    type: Schema.Types.ObjectId,
    required: true,
    ref: 'Mindmap',
    index: true
  },
  version: {
    type: Number,
    required: true
  },
  name: {
    type: String,
    trim: true,
    maxlength: 255
  },
  description: {
    type: String,
    trim: true,
    maxlength: 1000
  },
  snapshot: {
    type: Schema.Types.Mixed,
    required: true,
    description: 'Full mindmap snapshot'
  },
  createdBy: {
    type: Number,
    required: true
  },
  tags: [String],
  isAutoSave: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: { createdAt: true, updatedAt: false },
  collection: 'mindmap_versions'
});

MindmapVersionSchema.index({ mindmapId: 1, version: -1 });
MindmapVersionSchema.index({ mindmapId: 1, version: 1 }, { unique: true });

// ==============================================================================
// REALTIME SESSION SCHEMA
// ==============================================================================

const RealtimeSessionSchema = new Schema({
  mindmapId: {
    type: Schema.Types.ObjectId,
    required: true,
    ref: 'Mindmap',
    index: true
  },
  mysqlUserId: {
    type: Number,
    required: true,
    index: true
  },
  socketId: {
    type: String,
    required: true,
    index: true,
    unique: true
  },
  userInfo: {
    email: String,
    fullName: String,
    avatar: String,
    color: String
  },
  cursor: {
    x: Number,
    y: Number,
    nodeId: String
  },
  viewport: {
    x: Number,
    y: Number,
    zoom: Number
  },
  isActive: {
    type: Boolean,
    default: true,
    index: true
  },
  isEditing: {
    type: Boolean,
    default: false
  },
  lastActivity: {
    type: Date,
    default: Date.now,
    index: true
  }
}, {
  timestamps: { createdAt: 'connectedAt', updatedAt: false },
  collection: 'realtime_sessions'
});

RealtimeSessionSchema.index({ mindmapId: 1, isActive: 1 });
RealtimeSessionSchema.index({ lastActivity: 1 }, { expireAfterSeconds: 3600 }); // 1 hour

// ==============================================================================
// COLLABORATION INVITATION SCHEMA
// ==============================================================================

const CollaborationInvitationSchema = new Schema({
  mindmapId: {
    type: Schema.Types.ObjectId,
    required: true,
    ref: 'Mindmap',
    index: true
  },
  invitedByUserId: {
    type: Number,
    required: true
  },
  invitedEmail: {
    type: String,
    required: true,
    lowercase: true,
    trim: true,
    index: true
  },
  invitedUserId: {
    type: Number
  },
  role: {
    type: String,
    enum: ['editor', 'viewer'],
    required: true
  },
  status: {
    type: String,
    enum: ['pending', 'accepted', 'rejected', 'cancelled', 'expired'],
    default: 'pending',
    index: true
  },
  token: {
    type: String,
    unique: true,
    required: true
  },
  message: {
    type: String,
    maxlength: 500
  },
  expiresAt: {
    type: Date,
    required: true,
    index: true
  },
  acceptedAt: Date,
  metadata: {
    sentViaEmail: { type: Boolean, default: false },
    emailSentAt: Date,
    reminderCount: { type: Number, default: 0 }
  }
}, {
  timestamps: true,
  collection: 'collaboration_invitations'
});

CollaborationInvitationSchema.index({ mindmapId: 1, invitedEmail: 1 });
CollaborationInvitationSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

// ==============================================================================
// COMMENT SCHEMA
// ==============================================================================

const CommentSchema = new Schema({
  mindmapId: {
    type: Schema.Types.ObjectId,
    required: true,
    ref: 'Mindmap',
    index: true
  },
  nodeId: {
    type: String,
    required: true,
    index: true
  },
  mysqlUserId: {
    type: Number,
    required: true
  },
  content: {
    type: String,
    required: true,
    maxlength: 2000
  },
  mentions: [Number],
  resolved: {
    type: Boolean,
    default: false
  },
  resolvedBy: Number,
  resolvedAt: Date,
  parentCommentId: {
    type: Schema.Types.ObjectId,
    ref: 'Comment'
  },
  isEdited: {
    type: Boolean,
    default: false
  },
  editedAt: Date
}, {
  timestamps: true,
  collection: 'comments'
});

CommentSchema.index({ mindmapId: 1, nodeId: 1, createdAt: -1 });
CommentSchema.index({ mysqlUserId: 1 });
CommentSchema.index({ resolved: 1 });

// ==============================================================================
// ACTIVITY SCHEMA
// ==============================================================================

const MindmapActivitySchema = new Schema({
  mindmapId: {
    type: Schema.Types.ObjectId,
    required: true,
    ref: 'Mindmap',
    index: true
  },
  mysqlUserId: {
    type: Number,
    required: true,
    index: true
  },
  activityType: {
    type: String,
    enum: [
      'created', 'updated', 'viewed', 'shared', 'forked',
      'commented', 'collaborator_added', 'collaborator_removed',
      'version_created', 'exported', 'ai_generated', 'template_used'
    ],
    required: true,
    index: true
  },
  description: String,
  metadata: Schema.Types.Mixed
}, {
  timestamps: { createdAt: true, updatedAt: false },
  collection: 'mindmap_activities'
});

MindmapActivitySchema.index({ mindmapId: 1, createdAt: -1 });
MindmapActivitySchema.index({ createdAt: 1 }, { expireAfterSeconds: 15552000 }); // 180 days

// ==============================================================================
// TEMPLATE SCHEMA
// ==============================================================================

const TemplateSchema = new Schema({
  name: {
    type: String,
    required: true,
    trim: true,
    maxlength: 255
  },
  description: {
    type: String,
    trim: true,
    maxlength: 2000
  },
  thumbnail: String,
  category: {
    type: String,
    enum: ['work', 'personal', 'education', 'project', 'brainstorming', 'ai-generated', 'other'],
    default: 'other'
  },
  tags: [String],
  
  // ReactFlow template data
  templateData: {
    nodes: [nodeSchema],
    edges: [edgeSchema],
    viewport: {
      x: { type: Number, default: 0 },
      y: { type: Number, default: 0 },
      zoom: { type: Number, default: 1 }
    },
    settings: Schema.Types.Mixed
  },
  
  createdBy: Number,
  isOfficial: {
    type: Boolean,
    default: false
  },
  isPublic: {
    type: Boolean,
    default: true
  },
  aiWorkflowId: Number,
  usageCount: {
    type: Number,
    default: 0
  },
  status: {
    type: String,
    enum: ['active', 'archived', 'deleted'],
    default: 'active'
  }
}, {
  timestamps: true,
  collection: 'templates'
});

TemplateSchema.index({ category: 1, status: 1 });
TemplateSchema.index({ tags: 1 });
TemplateSchema.index({ isOfficial: 1, isPublic: 1 });
TemplateSchema.index({ usageCount: -1 });

// ==============================================================================
// CREATE MODELS
// ==============================================================================

const Mindmap = mongoose.model('Mindmap', MindmapSchema);
const MindmapHistory = mongoose.model('MindmapHistory', MindmapHistorySchema);
const MindmapVersion = mongoose.model('MindmapVersion', MindmapVersionSchema);
const RealtimeSession = mongoose.model('RealtimeSession', RealtimeSessionSchema);
const CollaborationInvitation = mongoose.model('CollaborationInvitation', CollaborationInvitationSchema);
const Comment = mongoose.model('Comment', CommentSchema);
const MindmapActivity = mongoose.model('MindmapActivity', MindmapActivitySchema);
const Template = mongoose.model('Template', TemplateSchema);

// ==============================================================================
// HELPER FUNCTIONS
// ==============================================================================

/**
 * Initialize database with indexes
 */
async function initializeDatabase() {
  try {
    console.log('Creating indexes...');
    
    await Mindmap.createIndexes();
    await MindmapHistory.createIndexes();
    await MindmapVersion.createIndexes();
    await RealtimeSession.createIndexes();
    await CollaborationInvitation.createIndexes();
    await Comment.createIndexes();
    await MindmapActivity.createIndexes();
    await Template.createIndexes();
    
    console.log('All indexes created successfully');
  } catch (error) {
    console.error('Error creating indexes:', error);
    throw error;
  }
}

/**
 * Transform ReactFlow data for MongoDB
 */
function prepareForSave(reactFlowData) {
  const { nodes, edges, viewport } = reactFlowData;
  
  return {
    nodes: nodes.map(node => ({
      id: node.id,
      type: node.type || 'default',
      position: node.position,
      data: node.data,
      style: node.style,
      className: node.className,
      draggable: node.draggable,
      selectable: node.selectable,
      connectable: node.connectable,
      deletable: node.deletable,
      dragHandle: node.dragHandle,
      parentNode: node.parentNode,
      extent: node.extent,
      expandParent: node.expandParent
    })),
    edges: edges.map(edge => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle,
      targetHandle: edge.targetHandle,
      type: edge.type || 'default',
      animated: edge.animated,
      label: edge.label,
      labelStyle: edge.labelStyle,
      labelShowBg: edge.labelShowBg,
      labelBgStyle: edge.labelBgStyle,
      labelBgPadding: edge.labelBgPadding,
      labelBgBorderRadius: edge.labelBgBorderRadius,
      style: edge.style,
      markerStart: edge.markerStart,
      markerEnd: edge.markerEnd,
      data: edge.data
    })),
    viewport: viewport || { x: 0, y: 0, zoom: 1 }
  };
}

/**
 * Transform MongoDB data for ReactFlow
 */
function prepareForReactFlow(mindmapDoc) {
  return {
    nodes: mindmapDoc.nodes.map(node => ({
      id: node.id,
      type: node.type,
      position: node.position,
      data: node.data,
      ...(node.style && { style: node.style }),
      ...(node.className && { className: node.className }),
      ...(node.draggable !== undefined && { draggable: node.draggable }),
      ...(node.selectable !== undefined && { selectable: node.selectable }),
      ...(node.connectable !== undefined && { connectable: node.connectable }),
      ...(node.deletable !== undefined && { deletable: node.deletable }),
      ...(node.dragHandle && { dragHandle: node.dragHandle }),
      ...(node.parentNode && { parentNode: node.parentNode }),
      ...(node.extent && { extent: node.extent }),
      ...(node.expandParent !== undefined && { expandParent: node.expandParent })
    })),
    edges: mindmapDoc.edges.map(edge => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      ...(edge.sourceHandle && { sourceHandle: edge.sourceHandle }),
      ...(edge.targetHandle && { targetHandle: edge.targetHandle }),
      type: edge.type,
      ...(edge.animated !== undefined && { animated: edge.animated }),
      ...(edge.label && { label: edge.label }),
      ...(edge.labelStyle && { labelStyle: edge.labelStyle }),
      ...(edge.labelShowBg !== undefined && { labelShowBg: edge.labelShowBg }),
      ...(edge.labelBgStyle && { labelBgStyle: edge.labelBgStyle }),
      ...(edge.labelBgPadding && { labelBgPadding: edge.labelBgPadding }),
      ...(edge.labelBgBorderRadius !== undefined && { labelBgBorderRadius: edge.labelBgBorderRadius }),
      ...(edge.style && { style: edge.style }),
      ...(edge.markerStart && { markerStart: edge.markerStart }),
      ...(edge.markerEnd && { markerEnd: edge.markerEnd }),
      ...(edge.data && { data: edge.data })
    })),
    viewport: mindmapDoc.viewport
  };
}

/**
 * Get user's mindmaps
 */
async function getUserMindmaps(userId, options = {}) {
  const {
    status = 'active',
    limit = 20,
    skip = 0,
    sortBy = 'updatedAt',
    sortOrder = -1,
    category = null,
    searchQuery = null
  } = options;
  
  const query = {
    $or: [
      { mysqlUserId: userId },
      { 'collaborators.mysqlUserId': userId, 'collaborators.status': 'accepted' }
    ],
    status
  };
  
  if (category) query.category = category;
  if (searchQuery) query.$text = { $search: searchQuery };
  
  return await Mindmap.find(query)
    .sort({ [sortBy]: sortOrder })
    .limit(limit)
    .skip(skip)
    .select('-nodes -edges'); // Exclude large arrays for list view
}

/**
 * Create mindmap from template
 */
async function createMindmapFromTemplate(templateId, userId, title) {
  const template = await Template.findById(templateId);
  if (!template) throw new Error('Template not found');
  
  const mindmap = new Mindmap({
    mysqlUserId: userId,
    title: title || template.name,
    description: template.description,
    nodes: template.templateData.nodes,
    edges: template.templateData.edges,
    viewport: template.templateData.viewport,
    settings: template.templateData.settings,
    category: template.category,
    tags: template.tags,
    collaborators: [{
      mysqlUserId: userId,
      role: 'owner',
      invitedBy: userId,
      status: 'accepted'
    }]
  });
  
  await mindmap.save();
  await Template.findByIdAndUpdate(templateId, { $inc: { usageCount: 1 } });
  
  return mindmap;
}

// ==============================================================================
// EXPORTS
// ==============================================================================

module.exports = {
  // Models
  Mindmap,
  MindmapHistory,
  MindmapVersion,
  RealtimeSession,
  CollaborationInvitation,
  Comment,
  MindmapActivity,
  Template,
  
  // Helper functions
  initializeDatabase,
  prepareForSave,
  prepareForReactFlow,
  getUserMindmaps,
  createMindmapFromTemplate
};

// ==============================================================================
// USAGE EXAMPLES WITH REACTFLOW
// ==============================================================================
/*

// 1. Save ReactFlow data to MongoDB
const { prepareForSave, Mindmap } = require('./mongo');

// ReactFlow instance data
const reactFlowInstance = reactFlowInstanceRef.current;
const { nodes, edges, viewport } = reactFlowInstance.toObject();

const mindmapData = prepareForSave({ nodes, edges, viewport });

const mindmap = new Mindmap({
  mysqlUserId: userId,
  title: 'My Mindmap',
  ...mindmapData,
  collaborators: [{
    mysqlUserId: userId,
    role: 'owner',
    invitedBy: userId,
    status: 'accepted'
  }]
});

await mindmap.save();


// 2. Load mindmap for ReactFlow
const { prepareForReactFlow, Mindmap } = require('./mongo');

const mindmap = await Mindmap.findById(mindmapId);
const reactFlowData = prepareForReactFlow(mindmap);

// Use in ReactFlow
setNodes(reactFlowData.nodes);
setEdges(reactFlowData.edges);
setViewport(reactFlowData.viewport);


// 3. Update mindmap from ReactFlow changes
const { nodes, edges, viewport } = reactFlowInstance.toObject();
const updateData = prepareForSave({ nodes, edges, viewport });

await Mindmap.findByIdAndUpdate(mindmapId, {
  ...updateData,
  'metadata.lastEditedBy': userId
});


// 4. Create from template
const mindmap = await createMindmapFromTemplate(templateId, userId, 'My Custom Title');
const reactFlowData = prepareForReactFlow(mindmap);

*/