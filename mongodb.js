// ==============================================================================
// FILE: mongodb_schema.js
// ==============================================================================
// MongoDB Schema for RiverFlow Mindmap Real-time System (using Mongoose)
// Version: 2.0 (Optimized)
// Description: Mindmap data, nodes, collaboration, history, real-time sessions
// ==============================================================================

const mongoose = require('mongoose');
const Schema = mongoose.Schema;

// ==============================================================================
// SUB-SCHEMAS (Embedded Documents)
// ==============================================================================

/**
 * Node Schema - Individual node in mindmap
 */
const nodeSchema = new Schema({
  id: {
    type: String,
    required: true,
    description: 'Unique node identifier (UUID)'
  },
  type: {
    type: String,
    enum: ['root', 'branch', 'leaf', 'floating'],
    default: 'branch',
    description: 'Type of node in the mindmap'
  },
  content: {
    text: { type: String, required: true },
    html: { type: String }, // Rich text content
    format: {
      fontSize: { type: Number, default: 14 },
      fontFamily: { type: String, default: 'Arial' },
      fontWeight: { type: String, default: 'normal' },
      fontStyle: { type: String, default: 'normal' },
      color: { type: String, default: '#000000' },
      backgroundColor: { type: String, default: '#FFFFFF' },
      borderColor: { type: String, default: '#CCCCCC' },
      borderWidth: { type: Number, default: 1 },
      borderRadius: { type: Number, default: 4 }
    }
  },
  position: {
    x: { type: Number, required: true, default: 0 },
    y: { type: Number, required: true, default: 0 },
    z: { type: Number, default: 0 } // Layer order
  },
  size: {
    width: { type: Number, default: 150 },
    height: { type: Number, default: 50 }
  },
  parent: {
    type: String,
    default: null,
    description: 'Parent node ID'
  },
  children: [{
    type: String,
    description: 'Array of child node IDs'
  }],
  metadata: {
    icon: { type: String }, // Icon name or URL
    image: { type: String }, // Image URL
    link: { type: String }, // External link
    tags: [{ type: String }], // Tags for categorization
    notes: { type: String }, // Additional notes
    priority: { type: Number, min: 1, max: 5, default: 3 },
    completed: { type: Boolean, default: false }
  },
  collapsed: {
    type: Boolean,
    default: false,
    description: 'Whether children are collapsed'
  },
  createdBy: {
    type: Number,
    required: true,
    description: 'MySQL user ID who created this node'
  },
  createdAt: {
    type: Date,
    default: Date.now
  },
  updatedAt: {
    type: Date,
    default: Date.now
  }
}, { _id: false });

/**
 * Edge Schema - Connection between nodes
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
  type: {
    type: String,
    enum: ['straight', 'curved', 'bezier', 'step'],
    default: 'curved'
  },
  style: {
    strokeColor: { type: String, default: '#999999' },
    strokeWidth: { type: Number, default: 2 },
    strokeStyle: {
      type: String,
      enum: ['solid', 'dashed', 'dotted'],
      default: 'solid'
    }
  },
  label: {
    text: { type: String },
    position: { type: Number, default: 0.5 } // 0-1, position along the edge
  },
  animated: {
    type: Boolean,
    default: false
  }
}, { _id: false });

/**
 * Collaborator Schema - Users who can access the mindmap
 */
const collaboratorSchema = new Schema({
  mysqlUserId: {
    type: Number,
    required: true,
    description: 'MySQL user ID of the collaborator'
  },
  role: {
    type: String,
    enum: ['owner', 'editor', 'viewer'],
    required: true,
    description: 'Permission level for this collaborator'
  },
  invitedBy: {
    type: Number,
    required: true,
    description: 'MySQL user ID who sent the invitation'
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
// MAIN SCHEMAS
// ==============================================================================

/**
 * Mindmap Schema - Main document for storing mindmaps
 */
const MindmapSchema = new Schema({
  mysqlUserId: {
    type: Number,
    required: true,
    index: true,
    description: 'Owner user ID from MySQL database'
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
    description: 'URL to mindmap thumbnail image'
  },
  nodes: {
    type: [nodeSchema],
    default: [],
    description: 'Array of nodes in the mindmap'
  },
  edges: {
    type: [edgeSchema],
    default: [],
    description: 'Array of edges connecting nodes'
  },
  settings: {
    theme: {
      type: String,
      enum: ['light', 'dark', 'auto'],
      default: 'light'
    },
    layout: {
      type: String,
      enum: ['tree', 'mind', 'org', 'radial', 'free'],
      default: 'mind'
    },
    direction: {
      type: String,
      enum: ['horizontal', 'vertical'],
      default: 'horizontal'
    },
    gridEnabled: {
      type: Boolean,
      default: true
    },
    snapToGrid: {
      type: Boolean,
      default: false
    },
    gridSize: {
      type: Number,
      default: 20
    },
    zoom: {
      type: Number,
      default: 1,
      min: 0.1,
      max: 3
    },
    canvasSize: {
      width: { type: Number, default: 5000 },
      height: { type: Number, default: 5000 }
    }
  },
  isPublic: {
    type: Boolean,
    default: false,
    index: true,
    description: 'Whether mindmap is publicly accessible'
  },
  shareToken: {
    type: String,
    unique: true,
    sparse: true,
    description: 'Token for public sharing link'
  },
  collaborators: {
    type: [collaboratorSchema],
    default: [],
    description: 'Users who have access to this mindmap'
  },
  tags: [{
    type: String,
    trim: true
  }],
  category: {
    type: String,
    enum: ['work', 'personal', 'education', 'project', 'brainstorming', 'ai-generated', 'other'],
    default: 'other'
  },
  isFavorite: {
    type: Boolean,
    default: false,
    description: 'Marked as favorite by owner'
  },
  isTemplate: {
    type: Boolean,
    default: false,
    description: 'Can be used as a template'
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
    default: false,
    description: 'Generated by AI workflow'
  },
  aiWorkflowId: {
    type: Number,
    description: 'MySQL AI workflow ID if generated by AI'
  },
  aiMetadata: {
    type: Schema.Types.Mixed,
    description: 'AI generation metadata (prompt, parameters, etc.)'
  },
  // Metadata
  metadata: {
    nodeCount: { type: Number, default: 0 },
    edgeCount: { type: Number, default: 0 },
    lastEditedBy: { type: Number }, // MySQL user ID
    viewCount: { type: Number, default: 0 },
    forkCount: { type: Number, default: 0 },
    forkedFrom: { type: Schema.Types.ObjectId, ref: 'Mindmap' }
  }
}, {
  timestamps: true, // Automatically adds createdAt and updatedAt
  collection: 'mindmaps'
});

// Indexes for performance
MindmapSchema.index({ mysqlUserId: 1, status: 1 });
MindmapSchema.index({ title: 'text', description: 'text' }); // Full-text search
MindmapSchema.index({ tags: 1 });
MindmapSchema.index({ isPublic: 1, status: 1 });
MindmapSchema.index({ 'collaborators.mysqlUserId': 1 });
MindmapSchema.index({ createdAt: -1 });
MindmapSchema.index({ updatedAt: -1 });
MindmapSchema.index({ aiWorkflowId: 1 });
MindmapSchema.index({ category: 1 });

// Virtual for getting collaborator count
MindmapSchema.virtual('collaboratorCount').get(function() {
  return this.collaborators.filter(c => c.status === 'accepted').length;
});

// Pre-save hook to update metadata
MindmapSchema.pre('save', function(next) {
  this.metadata.nodeCount = this.nodes.length;
  this.metadata.edgeCount = this.edges.length;
  next();
});

// ==============================================================================
// MINDMAP HISTORY SCHEMA
// ==============================================================================
/**
 * Track all changes to mindmaps for undo/redo and audit trail
 */
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
    index: true,
    description: 'User who made the change'
  },
  action: {
    type: String,
    enum: [
      'create',
      'update',
      'delete',
      'node_add',
      'node_update',
      'node_delete',
      'node_move',
      'edge_add',
      'edge_update',
      'edge_delete',
      'settings_update',
      'collaborator_add',
      'collaborator_remove',
      'restore',
      'ai_generate'
    ],
    required: true,
    index: true
  },
  changes: {
    type: Schema.Types.Mixed,
    description: 'Delta/diff of what changed (for efficient storage)'
  },
  snapshot: {
    type: Schema.Types.Mixed,
    description: 'Full snapshot (stored periodically, not for every change)'
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

// Indexes
MindmapHistorySchema.index({ mindmapId: 1, createdAt: -1 });
MindmapHistorySchema.index({ mysqlUserId: 1, createdAt: -1 });
MindmapHistorySchema.index({ action: 1 });

// TTL index - automatically delete history older than 90 days (configurable)
MindmapHistorySchema.index(
  { createdAt: 1 }, 
  { expireAfterSeconds: 7776000 } // 90 days
);

// ==============================================================================
// MINDMAP VERSIONS SCHEMA
// ==============================================================================
/**
 * Store major versions of mindmaps (snapshots)
 */
const MindmapVersionSchema = new Schema({
  mindmapId: {
    type: Schema.Types.ObjectId,
    required: true,
    ref: 'Mindmap',
    index: true
  },
  version: {
    type: Number,
    required: true,
    description: 'Version number (incremental)'
  },
  name: {
    type: String,
    trim: true,
    maxlength: 255,
    description: 'Optional version name/label'
  },
  description: {
    type: String,
    trim: true,
    maxlength: 1000
  },
  snapshot: {
    type: Schema.Types.Mixed,
    required: true,
    description: 'Complete snapshot of the mindmap at this version'
  },
  createdBy: {
    type: Number,
    required: true,
    description: 'MySQL user ID who created this version'
  },
  tags: [{
    type: String,
    description: 'Version tags like "stable", "draft", "reviewed"'
  }],
  isAutoSave: {
    type: Boolean,
    default: false,
    description: 'Whether this was an automatic save'
  }
}, {
  timestamps: { createdAt: true, updatedAt: false },
  collection: 'mindmap_versions'
});

// Indexes
MindmapVersionSchema.index({ mindmapId: 1, version: -1 });
MindmapVersionSchema.index({ mindmapId: 1, createdAt: -1 });
MindmapVersionSchema.index({ mindmapId: 1, version: 1 }, { unique: true });

// ==============================================================================
// REALTIME SESSIONS SCHEMA
// ==============================================================================
/**
 * Track active real-time editing sessions
 * Used for showing who's online and cursor positions
 */
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
    description: 'Socket.IO connection ID'
  },
  userInfo: {
    email: { type: String },
    fullName: { type: String },
    avatar: { type: String },
    color: { type: String, description: 'Assigned color for cursor and selections' }
  },
  cursor: {
    x: { type: Number },
    y: { type: Number },
    nodeId: { type: String, description: 'Currently selected/editing node' }
  },
  viewport: {
    x: { type: Number },
    y: { type: Number },
    zoom: { type: Number }
  },
  isActive: {
    type: Boolean,
    default: true,
    index: true
  },
  isEditing: {
    type: Boolean,
    default: false,
    description: 'Currently making changes'
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

// Indexes
RealtimeSessionSchema.index({ mindmapId: 1, isActive: 1 });
RealtimeSessionSchema.index({ socketId: 1 }, { unique: true });

// TTL index - automatically remove inactive sessions after 1 hour
RealtimeSessionSchema.index(
  { lastActivity: 1 },
  { expireAfterSeconds: 3600 }
);

// ==============================================================================
// COLLABORATION INVITATIONS SCHEMA
// ==============================================================================
/**
 * Track collaboration invitations (separate from embedded collaborators)
 * Useful for managing pending invitations and notifications
 */
const CollaborationInvitationSchema = new Schema({
  mindmapId: {
    type: Schema.Types.ObjectId,
    required: true,
    ref: 'Mindmap',
    index: true
  },
  invitedByUserId: {
    type: Number,
    required: true,
    description: 'MySQL user ID who sent invitation'
  },
  invitedEmail: {
    type: String,
    required: true,
    lowercase: true,
    trim: true,
    index: true
  },
  invitedUserId: {
    type: Number,
    description: 'MySQL user ID if user exists'
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
    required: true,
    description: 'Invitation token for email link'
  },
  message: {
    type: String,
    maxlength: 500,
    description: 'Personal message from inviter'
  },
  expiresAt: {
    type: Date,
    required: true,
    index: true
  },
  acceptedAt: {
    type: Date
  },
  metadata: {
    sentViaEmail: { type: Boolean, default: false },
    emailSentAt: { type: Date },
    reminderCount: { type: Number, default: 0 }
  }
}, {
  timestamps: true,
  collection: 'collaboration_invitations'
});

// Indexes
CollaborationInvitationSchema.index({ mindmapId: 1, invitedEmail: 1 });
CollaborationInvitationSchema.index({ invitedUserId: 1, status: 1 });
CollaborationInvitationSchema.index({ token: 1 });

// TTL index - automatically delete expired invitations
CollaborationInvitationSchema.index(
  { expiresAt: 1 },
  { expireAfterSeconds: 0 }
);

// ==============================================================================
// MINDMAP COMMENTS SCHEMA
// ==============================================================================
/**
 * Comments on mindmap nodes (for collaboration)
 */
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
    index: true,
    description: 'Node this comment is attached to'
  },
  mysqlUserId: {
    type: Number,
    required: true,
    description: 'Comment author'
  },
  content: {
    type: String,
    required: true,
    maxlength: 2000
  },
  mentions: [{
    type: Number,
    description: 'MySQL user IDs mentioned in comment'
  }],
  resolved: {
    type: Boolean,
    default: false
  },
  resolvedBy: {
    type: Number,
    description: 'MySQL user ID who resolved'
  },
  resolvedAt: {
    type: Date
  },
  parentCommentId: {
    type: Schema.Types.ObjectId,
    ref: 'Comment',
    description: 'For threaded replies'
  },
  isEdited: {
    type: Boolean,
    default: false
  },
  editedAt: {
    type: Date
  }
}, {
  timestamps: true,
  collection: 'comments'
});

// Indexes
CommentSchema.index({ mindmapId: 1, nodeId: 1, createdAt: -1 });
CommentSchema.index({ mysqlUserId: 1 });
CommentSchema.index({ resolved: 1 });
CommentSchema.index({ mentions: 1 });

// ==============================================================================
// MINDMAP ACTIVITIES SCHEMA
// ==============================================================================
/**
 * Activity feed for mindmaps (for notifications and activity log)
 */
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
      'created',
      'updated',
      'viewed',
      'shared',
      'forked',
      'commented',
      'collaborator_added',
      'collaborator_removed',
      'version_created',
      'exported',
      'ai_generated',
      'template_used'
    ],
    required: true,
    index: true
  },
  description: {
    type: String,
    description: 'Human-readable description of the activity'
  },
  metadata: {
    type: Schema.Types.Mixed,
    description: 'Additional context about the activity'
  }
}, {
  timestamps: { createdAt: true, updatedAt: false },
  collection: 'mindmap_activities'
});

// Indexes
MindmapActivitySchema.index({ mindmapId: 1, createdAt: -1 });
MindmapActivitySchema.index({ mysqlUserId: 1, createdAt: -1 });
MindmapActivitySchema.index({ activityType: 1, createdAt: -1 });

// TTL index - delete activities older than 180 days
MindmapActivitySchema.index(
  { createdAt: 1 },
  { expireAfterSeconds: 15552000 } // 180 days
);

// ==============================================================================
// TEMPLATES SCHEMA
// ==============================================================================
/**
 * Mindmap templates for quick start
 */
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
  thumbnail: {
    type: String,
    description: 'URL to template thumbnail'
  },
  category: {
    type: String,
    enum: ['work', 'personal', 'education', 'project', 'brainstorming', 'ai-generated', 'other'],
    default: 'other'
  },
  tags: [{ type: String, trim: true }],
  
  // Template content (same structure as mindmap)
  templateData: {
    nodes: [nodeSchema],
    edges: [edgeSchema],
    settings: { type: Schema.Types.Mixed }
  },
  
  // Template metadata
  createdBy: {
    type: Number,
    description: 'MySQL user ID who created the template'
  },
  isOfficial: {
    type: Boolean,
    default: false,
    description: 'Official template from system'
  },
  isPublic: {
    type: Boolean,
    default: true
  },
  
  // AI Integration
  aiWorkflowId: {
    type: Number,
    description: 'Associated AI workflow ID'
  },
  
  // Usage statistics
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

// Indexes
TemplateSchema.index({ category: 1, status: 1 });
TemplateSchema.index({ tags: 1 });
TemplateSchema.index({ isOfficial: 1, isPublic: 1 });
TemplateSchema.index({ usageCount: -1 });
TemplateSchema.index({ aiWorkflowId: 1 });

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
 * Clean up expired sessions
 */
async function cleanupExpiredSessions() {
  try {
    const oneHourAgo = new Date(Date.now() - 3600000);
    
    const result = await RealtimeSession.deleteMany({
      lastActivity: { $lt: oneHourAgo }
    });
    
    console.log(`Cleaned up ${result.deletedCount} expired sessions`);
    return result;
  } catch (error) {
    console.error('Error cleaning up sessions:', error);
    throw error;
  }
}

/**
 * Create a new version snapshot
 */
async function createVersionSnapshot(mindmapId, userId, versionName, description) {
  try {
    const mindmap = await Mindmap.findById(mindmapId);
    if (!mindmap) {
      throw new Error('Mindmap not found');
    }
    
    // Get latest version number
    const latestVersion = await MindmapVersion
      .findOne({ mindmapId })
      .sort({ version: -1 })
      .limit(1);
    
    const newVersionNumber = latestVersion ? latestVersion.version + 1 : 1;
    
    const version = new MindmapVersion({
      mindmapId,
      version: newVersionNumber,
      name: versionName,
      description,
      snapshot: mindmap.toObject(),
      createdBy: userId
    });
    
    await version.save();
    console.log(`Created version ${newVersionNumber} for mindmap ${mindmapId}`);
    return version;
  } catch (error) {
    console.error('Error creating version:', error);
    throw error;
  }
}

/**
 * Get active collaborators for a mindmap
 */
async function getActiveCollaborators(mindmapId) {
  try {
    const sessions = await RealtimeSession.find({
      mindmapId,
      isActive: true,
      lastActivity: { $gt: new Date(Date.now() - 300000) } // Last 5 minutes
    }).sort({ connectedAt: -1 });
    
    return sessions;
  } catch (error) {
    console.error('Error getting active collaborators:', error);
    throw error;
  }
}

/**
 * Create mindmap from template
 */
async function createMindmapFromTemplate(templateId, userId, title) {
  try {
    const template = await Template.findById(templateId);
    if (!template) {
      throw new Error('Template not found');
    }
    
    const mindmap = new Mindmap({
      mysqlUserId: userId,
      title: title || template.name,
      description: template.description,
      nodes: template.templateData.nodes,
      edges: template.templateData.edges,
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
    
    // Update template usage count
    await Template.findByIdAndUpdate(templateId, { $inc: { usageCount: 1 } });
    
    console.log(`Created mindmap from template ${templateId}`);
    return mindmap;
  } catch (error) {
    console.error('Error creating mindmap from template:', error);
    throw error;
  }
}

/**
 * Get user's mindmaps (owned + collaborated)
 */
async function getUserMindmaps(userId, options = {}) {
  try {
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
    
    if (category) {
      query.category = category;
    }
    
    if (searchQuery) {
      query.$text = { $search: searchQuery };
    }
    
    const mindmaps = await Mindmap.find(query)
      .sort({ [sortBy]: sortOrder })
      .limit(limit)
      .skip(skip)
      .select('-nodes -edges'); // Exclude large arrays for list view
    
    return mindmaps;
  } catch (error) {
    console.error('Error getting user mindmaps:', error);
    throw error;
  }
}

/**
 * Record activity
 */
async function recordActivity(mindmapId, userId, activityType, description, metadata = {}) {
  try {
    const activity = new MindmapActivity({
      mindmapId,
      mysqlUserId: userId,
      activityType,
      description,
      metadata
    });
    
    await activity.save();
    return activity;
  } catch (error) {
    console.error('Error recording activity:', error);
    throw error;
  }
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
  cleanupExpiredSessions,
  createVersionSnapshot,
  getActiveCollaborators,
  createMindmapFromTemplate,
  getUserMindmaps,
  recordActivity
};

// ==============================================================================
// USAGE EXAMPLES
// ==============================================================================
/*

// 1. Connect to MongoDB
const mongoose = require('mongoose');

mongoose.connect('mongodb://localhost:27017/riverflow_mindmap', {
  useNewUrlParser: true,
  useUnifiedTopology: true
})
.then(() => {
  console.log('Connected to MongoDB');
  return initializeDatabase();
})
.catch(err => console.error('MongoDB connection error:', err));


// 2. Create a new mindmap
const { Mindmap } = require('./mongodb_schema');

const newMindmap = new Mindmap({
  mysqlUserId: 1,
  title: 'My First Mindmap',
  description: 'A simple example mindmap',
  nodes: [
    {
      id: 'node-1',
      type: 'root',
      content: {
        text: 'Central Idea',
        format: {
          fontSize: 18,
          fontWeight: 'bold',
          backgroundColor: '#4A90E2'
        }
      },
      position: { x: 0, y: 0 },
      size: { width: 200, height: 60 },
      createdBy: 1
    }
  ],
  collaborators: [
    {
      mysqlUserId: 1,
      role: 'owner',
      invitedBy: 1,
      status: 'accepted'
    }
  ]
});

await newMindmap.save();


// 3. Create mindmap from AI workflow
const aiMindmap = new Mindmap({
  mysqlUserId: 1,
  title: 'AI Generated: Skill Development Plan',
  aiGenerated: true,
  aiWorkflowId: 123, // MySQL workflow ID
  aiMetadata: {
    prompt: 'Create skill development plan for React',
    workflow: 'skill-development-plan'
  },
  nodes: [...], // Generated nodes
  collaborators: [{
    mysqlUserId: 1,
    role: 'owner',
    invitedBy: 1,
    status: 'accepted'
  }]
});

await aiMindmap.save();


// 4. Get user's mindmaps
const mindmaps = await getUserMindmaps(userId, {
  limit: 10,
  category: 'work',
  searchQuery: 'project planning'
});


// 5. Create from template
const mindmap = await createMindmapFromTemplate(
  templateId,
  userId,
  'My Custom Title'
);


// 6. Record activity
await recordActivity(
  mindmapId,
  userId,
  'ai_generated',
  'Generated mindmap using AI workflow',
  { workflowId: 123, workflowName: 'Skill Development Plan' }
);

*/

// ==============================================================================
// END OF SCHEMA
// ==============================================================================
